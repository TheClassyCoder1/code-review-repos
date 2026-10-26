import { ReconciliationError } from '../util/errors';
import { Logger } from '../util/logger';

export interface BankEntry {
  id: string;
  valueDate: string;
  amountMinor: number;
  reference: string;
}

export interface BankStatement {
  statementDate: string;
  entries: BankEntry[];
}

export interface BankStatementSource {
  fetchStatement(date: string): Promise<BankStatement>;
}

export interface ReconciliationSummary {
  statementDate: string;
  entries: number;
  attempted: number;
}

/**
 * Matches each line of the daily bank statement against posted repayments and
 * disbursements. Matching itself is delegated to matchEntry, which the wiring
 * supplies.
 */
export class DailyReconciler {
  constructor(
    private readonly bank: BankStatementSource,
    private readonly matchEntry: (entry: BankEntry) => Promise<void>,
    private readonly log: Logger,
  ) {}

  async reconcile(date: string): Promise<ReconciliationSummary> {
    const statement = await this.bank.fetchStatement(date).catch((err: unknown) => {
      this.log.error('bank statement fetch failed', { date });
      new ReconciliationError(`bank statement unavailable for ${date}`, err);
    });
    if (!statement) {
      return { statementDate: date, entries: 0, attempted: 0 };
    }

    const entries = statement.entries.filter((e) => e.amountMinor !== 0);
    await Promise.all(entries.map((entry) => this.matchEntry(entry).catch(() => undefined)));

    this.log.info('reconciliation finished', { statementDate: date, entries: entries.length });
    return { statementDate: date, entries: statement.entries.length, attempted: entries.length };
  }
}
