import { Cents, IsoDate, LedgerEntryType, RepaymentSchedule, ServicedLoan, LoanState } from '../domain/types';
import { LedgerRepository } from '../repository/ledger.repository';
import { BusinessCalendar, addDays } from '../util/business-calendar';
import { HttpStatusError, isTransientHttpError, withRetry } from '../util/retry';
import { Logger } from './collections-queue.service';
import { outstandingFor } from './payment-allocation.service';

export interface AchDebitRequest {
  loanId: number;
  amount: Cents;
  settlementDate: IsoDate;
  idempotencyKey: string;
}

export interface AchGateway {
  /** Resolves with the processor's trace id; rejects with HttpStatusError on non-2xx. */
  submitDebit(request: AchDebitRequest): Promise<string>;
}

export interface AutopayRunResult {
  loanId: number;
  status: 'SUBMITTED' | 'SKIPPED' | 'FAILED';
  amount: Cents;
  traceId?: string;
  reason?: string;
}

/** ACH files are cut this many calendar days before the due date. */
const AUTOPAY_LEAD_DAYS = 3;

export class AutopayService {
  constructor(
    private readonly gateway: AchGateway,
    private readonly ledger: LedgerRepository,
    private readonly calendar: BusinessCalendar,
    private readonly logger: Logger,
  ) {}

  /** Installment that autopay should collect on `runDate`, if any. */
  dueForRun(schedule: RepaymentSchedule, runDate: IsoDate) {
    const target = addDays(runDate, AUTOPAY_LEAD_DAYS);
    return schedule.installments.find((inst) => {
      const o = outstandingFor(inst);
      return inst.dueDate === target && o.fees + o.interest + o.principal > 0;
    });
  }

  /**
   * Submits the debit for the installment falling due `AUTOPAY_LEAD_DAYS` after
   * `runDate`. The idempotency key is scoped to loan + installment so a rerun of the
   * same batch never debits twice, and the payment is only posted once the processor
   * has accepted the file.
   */
  async scheduleRun(loan: ServicedLoan, schedule: RepaymentSchedule, runDate: IsoDate): Promise<AutopayRunResult> {
    if (!loan.autopayEnabled || loan.state !== LoanState.Active) {
      return { loanId: loan.loanId, status: 'SKIPPED', amount: 0, reason: 'NOT_ELIGIBLE' };
    }
    const inst = this.dueForRun(schedule, runDate);
    if (!inst) {
      return { loanId: loan.loanId, status: 'SKIPPED', amount: 0, reason: 'NOTHING_DUE' };
    }
    const o = outstandingFor(inst);
    const amount = o.fees + o.interest + o.principal;
    const idempotencyKey = `autopay:${loan.loanId}:${inst.sequence}`;
    const settlementDate = this.calendar.settlementDateFor(inst.dueDate);

    try {
      const traceId = await withRetry(
        () => this.gateway.submitDebit({ loanId: loan.loanId, amount, settlementDate, idempotencyKey }),
        { attempts: 3, baseDelayMs: 200, maxDelayMs: 2_000, isRetryable: isTransientHttpError },
      );
      this.ledger.postEntry({
        loanId: loan.loanId,
        type: LedgerEntryType.Payment,
        amount: -amount,
        effectiveDate: settlementDate,
        idempotencyKey,
      });
      this.logger.info('autopay debit submitted', { loanId: loan.loanId, installment: inst.sequence, traceId });
      return { loanId: loan.loanId, status: 'SUBMITTED', amount, traceId };
    } catch (error) {
      const reason = error instanceof HttpStatusError ? `HTTP_${error.status}` : 'GATEWAY_ERROR';
      this.logger.warn('autopay debit failed', { loanId: loan.loanId, installment: inst.sequence, reason });
      return { loanId: loan.loanId, status: 'FAILED', amount, reason };
    }
  }
}
