import { mkdir } from 'fs/promises';
import { Readable } from 'stream';
import { pipeline } from 'stream/promises';
import { describeError } from '../util/errors';
import { Logger } from '../util/logger';
import { openStatementFile, StatementLine, toCsvRow } from './statement-export';

export interface StatementLineSource {
  linesFor(accountId: string, period: string): AsyncIterable<StatementLine>;
}

async function* csvRows(lines: AsyncIterable<StatementLine>): AsyncIterable<string> {
  for await (const line of lines) {
    yield toCsvRow(line);
  }
}

/**
 * Monthly statement export. pipeline() owns the file stream for its whole
 * life: it ends the stream when the rows are exhausted and destroys it (closing
 * the descriptor) if the source or the write fails.
 */
export class StatementJob {
  constructor(
    private readonly source: StatementLineSource,
    private readonly exportDir: string,
    private readonly log: Logger,
  ) {}

  async exportOne(accountId: string, period: string): Promise<void> {
    const out = openStatementFile(this.exportDir, accountId, period);
    await pipeline(Readable.from(csvRows(this.source.linesFor(accountId, period))), out);
  }

  async exportAll(accountIds: string[], period: string): Promise<{ exported: number; failed: number }> {
    await mkdir(this.exportDir, { recursive: true });
    let exported = 0;
    let failed = 0;
    for (const accountId of accountIds) {
      try {
        await this.exportOne(accountId, period);
        exported++;
      } catch (err) {
        failed++;
        this.log.warn('statement export failed', { accountId, period, error: describeError(err) });
      }
    }
    this.log.info('statement export finished', { period, exported, failed });
    return { exported, failed };
  }
}
