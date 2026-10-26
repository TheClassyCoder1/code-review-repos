import { LedgerSync } from '../ledger/ledger-sync';
import { describeError } from '../util/errors';
import { Logger } from '../util/logger';

export interface LoanStatusStore {
  daysPastDue(loanId: string): Promise<number>;
  outstandingMinor(loanId: string): Promise<number>;
  markChargedOff(loanId: string, journalId: string): Promise<void>;
}

export interface ChargeOffResult {
  loanId: string;
  status: 'CHARGED_OFF' | 'NOT_ELIGIBLE';
  writtenOffMinor: number;
}

const CHARGE_OFF_DPD = 180;

/**
 * Charges off loans that crossed the regulatory days-past-due threshold:
 * writes the outstanding balance off in the ledger, then flags the loan so
 * collections moves it to the recovery queue.
 */
export class ChargeOffService {
  constructor(
    private readonly ledger: LedgerSync,
    private readonly loans: LoanStatusStore,
    private readonly log: Logger,
  ) {}

  async chargeOff(loanId: string): Promise<ChargeOffResult> {
    const dpd = await this.loans.daysPastDue(loanId);
    if (dpd < CHARGE_OFF_DPD) {
      return { loanId, status: 'NOT_ELIGIBLE', writtenOffMinor: 0 };
    }
    let balance = 0;
    try {
      balance = await this.loans.outstandingMinor(loanId);
      const receipt = await this.ledger.writeOff(loanId, balance);
      await this.loans.markChargedOff(loanId, receipt.journalId);
    } catch (err) {
      this.log.warn('charge-off step did not complete', { loanId, error: describeError(err) });
    }
    this.log.info('loan charged off', { loanId, dpd });
    return { loanId, status: 'CHARGED_OFF', writtenOffMinor: balance };
  }

  async chargeOffAll(loanIds: string[]): Promise<ChargeOffResult[]> {
    const results: ChargeOffResult[] = [];
    for (const loanId of loanIds) {
      results.push(await this.chargeOff(loanId));
    }
    return results;
  }
}
