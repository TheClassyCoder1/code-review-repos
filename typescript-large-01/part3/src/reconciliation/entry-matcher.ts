import { BankEntry } from './reconciler';

/** Matches one bank entry to a posted repayment or disbursement; throws when it cannot. */
export interface EntryMatcher {
  match(entry: BankEntry): Promise<void>;
}
