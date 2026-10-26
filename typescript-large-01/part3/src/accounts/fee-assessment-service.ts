import { LedgerSync } from '../ledger/ledger-sync';
import { Logger } from '../util/logger';
import { daysBetween, isoDate } from '../util/time';
import { ServicingAccount, ServicingAccountRepository } from './servicing-account-repository';

export interface FeePolicy {
  /** Looks up the late-fee schedule for the account's product; may call the pricing service. */
  lateFeeFor(account: ServicingAccount, asOf: Date): Promise<number>;
}

const GRACE_DAYS = 10;

/**
 * Nightly late-fee assessment. Runs alongside payment posting, which updates
 * the same account rows during the day and overnight.
 */
export class FeeAssessmentService {
  constructor(
    private readonly accounts: ServicingAccountRepository,
    private readonly feePolicy: FeePolicy,
    private readonly ledger: LedgerSync,
    private readonly log: Logger,
  ) {}

  /** Returns the fee charged, or 0 when the account is within grace or already settled. */
  async assessLateFee(accountId: string, dueDate: Date, asOf: Date): Promise<number> {
    if (daysBetween(dueDate, asOf) <= GRACE_DAYS) {
      return 0;
    }
    const account = await this.accounts.load(accountId);
    if (account.balanceMinor <= 0) {
      return 0;
    }
    const fee = await this.feePolicy.lateFeeFor(account, asOf);
    if (fee <= 0) {
      return 0;
    }
    account.feesMinor += fee;
    account.balanceMinor += fee;
    await this.accounts.overwrite(account);
    await this.ledger.post(account.loanId, 'LATE_FEE', fee, `fee-${accountId}-${isoDate(asOf)}`, asOf);
    this.log.info('late fee assessed', { accountId, feeMinor: fee });
    return fee;
  }

  async assessAll(dues: Array<{ accountId: string; dueDate: Date }>, asOf: Date): Promise<number> {
    const fees = await Promise.all(dues.map((d) => this.assessLateFee(d.accountId, d.dueDate, asOf)));
    return fees.reduce((sum, f) => sum + f, 0);
  }
}
