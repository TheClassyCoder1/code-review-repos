import {
  IsoDate,
  LedgerEntry,
  LedgerEntryType,
  RepaymentSchedule,
  Statement,
  StatementLine,
} from '../domain/types';
import { LedgerRepository } from '../repository/ledger.repository';
import { addDays } from '../util/business-calendar';
import { formatCents } from '../util/money';
import { outstandingFor } from './payment-allocation.service';

const DESCRIPTIONS: Record<LedgerEntryType, string> = {
  [LedgerEntryType.Disbursement]: 'Loan disbursement',
  [LedgerEntryType.Payment]: 'Payment received - thank you',
  [LedgerEntryType.InterestAccrual]: 'Interest charged',
  [LedgerEntryType.Fee]: 'Fee assessed',
  [LedgerEntryType.FeeWaiver]: 'Fee waived',
  [LedgerEntryType.Reversal]: 'Adjustment',
};

/** Monthly borrower statements built from the servicing ledger. */
export class StatementService {
  constructor(private readonly ledger: LedgerRepository) {}

  /**
   * Builds the statement for [periodStart, periodEnd]. Opening balance is the ledger
   * balance at the end of the day before the period, so every entry lands in exactly
   * one statement. The next payment is the earliest installment after the period
   * that still has something owing.
   */
  buildStatement(
    schedule: RepaymentSchedule,
    periodStart: IsoDate,
    periodEnd: IsoDate,
  ): Statement {
    if (periodEnd < periodStart) {
      throw new RangeError(`periodEnd ${periodEnd} is before periodStart ${periodStart}`);
    }
    const loanId = schedule.loanId;
    const openingBalance = this.ledger.balanceAsOf(loanId, addDays(periodStart, -1));
    const entries = this.ledger.entriesBetween(loanId, periodStart, periodEnd);

    const lines = this.toLines(entries, openingBalance);
    const closingBalance = lines.length > 0 ? lines[lines.length - 1].runningBalance : openingBalance;

    const owing = schedule.installments
      .map((inst) => ({ inst, o: outstandingFor(inst) }))
      .find(({ inst, o }) => inst.dueDate > periodEnd && o.fees + o.interest + o.principal > 0);

    return {
      loanId,
      periodStart,
      periodEnd,
      openingBalance,
      closingBalance,
      lines,
      nextPaymentDue: owing
        ? { dueDate: owing.inst.dueDate, amount: owing.o.fees + owing.o.interest + owing.o.principal }
        : undefined,
    };
  }

  /**
   * Plain-text rendering for the paper/PDF channel. Uses fixed-width columns and only
   * loan-level data; borrower contact details are merged in by the print vendor.
   */
  renderText(statement: Statement): string {
    const header = [
      `Loan ${statement.loanId}`,
      `Statement period ${statement.periodStart} to ${statement.periodEnd}`,
      `Opening balance ${formatCents(statement.openingBalance)}`,
      '',
      `${'Date'.padEnd(12)}${'Description'.padEnd(32)}${'Amount'.padStart(16)}${'Balance'.padStart(16)}`,
    ];
    const body = statement.lines.map(
      (l) =>
        `${l.date.padEnd(12)}${l.description.slice(0, 31).padEnd(32)}` +
        `${formatCents(l.amount).padStart(16)}${formatCents(l.runningBalance).padStart(16)}`,
    );
    const footer = [
      '',
      `Closing balance ${formatCents(statement.closingBalance)}`,
    ];
    if (statement.nextPaymentDue) {
      footer.push(
        `Next payment of ${formatCents(statement.nextPaymentDue.amount)} due ${statement.nextPaymentDue.dueDate}`,
      );
    }
    return [...header, ...body, ...footer].join('\n');
  }

  private toLines(entries: readonly LedgerEntry[], openingBalance: number): StatementLine[] {
    let running = openingBalance;
    return entries.map((e) => {
      running += e.amount;
      return {
        date: e.effectiveDate,
        description: DESCRIPTIONS[e.type],
        amount: e.amount,
        runningBalance: running,
      };
    });
  }
}
