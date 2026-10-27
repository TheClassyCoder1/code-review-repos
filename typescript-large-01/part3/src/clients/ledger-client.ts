import { readJson } from '../http/timed-fetch';
import { UpstreamError } from '../util/errors';

export type LedgerEntryKind = 'REPAYMENT' | 'LATE_FEE' | 'WRITE_OFF' | 'DISBURSEMENT' | 'REVERSAL';

export interface LedgerEntry {
  entryRef: string;
  loanId: string;
  kind: LedgerEntryKind;
  amountMinor: number;
  effectiveDate: string;
}

export interface LedgerReceipt {
  entryRef: string;
  journalId: string;
  postedAt: string;
}

/**
 * Thin transport over the general-ledger API. Request policy (deadlines,
 * cancellation) is owned by the caller and passed through as RequestInit.
 */
export class LedgerClient {
  constructor(private readonly baseUrl: string) {}

  async postEntry(entry: LedgerEntry, init: RequestInit): Promise<LedgerReceipt> {
    const res = await fetch(`${this.baseUrl}/v1/journal/entries`, {
      ...init,
      method: 'POST',
      headers: { 'content-type': 'application/json', 'idempotency-key': entry.entryRef },
      body: JSON.stringify(entry),
    });
    if (!res.ok) {
      throw new UpstreamError(`ledger rejected entry ${entry.entryRef}`, res.status);
    }
    return readJson<LedgerReceipt>(res, 'ledger receipt');
  }
}
