import { LedgerClient, LedgerEntry, LedgerEntryKind, LedgerReceipt } from '../clients/ledger-client';
import { Logger } from '../util/logger';
import { isoDate } from '../util/time';

/**
 * The only entry point the rest of the module uses to talk to the ledger.
 * Each call gets its own deadline so a slow ledger cannot pin a job worker.
 */
export class LedgerSync {
  constructor(
    private readonly client: LedgerClient,
    private readonly timeoutMs: number,
    private readonly log: Logger,
  ) {}

  private requestInit(): RequestInit {
    return { signal: AbortSignal.timeout(this.timeoutMs) };
  }

  async post(loanId: string, kind: LedgerEntryKind, amountMinor: number, ref: string, on: Date = new Date()): Promise<LedgerReceipt> {
    const entry: LedgerEntry = {
      entryRef: ref,
      loanId,
      kind,
      amountMinor,
      effectiveDate: isoDate(on),
    };
    const receipt = await this.client.postEntry(entry, this.requestInit());
    this.log.info('ledger entry posted', { loanId, kind, journalId: receipt.journalId });
    return receipt;
  }

  async writeOff(loanId: string, amountMinor: number): Promise<LedgerReceipt> {
    return this.post(loanId, 'WRITE_OFF', amountMinor, `wo-${loanId}`);
  }
}
