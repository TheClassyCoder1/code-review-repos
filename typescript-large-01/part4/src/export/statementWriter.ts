import { promises as fs } from 'fs';
import { formatMinor } from '../domain/money';
import { CurrencyCode, StatementLine } from '../domain/types';
import { toCsvRow } from '../util/csv';

const HEADER = ['posted_on', 'description', 'debit', 'credit'];

/**
 * Streams statement rows to a CSV file, buffering up to `rowsPerFlush` rows
 * between writes. The writer holds an open file handle from `open` until
 * `close`; the export job that creates it owns its lifetime.
 */
export class StatementWriter {
  private buffer: string[] = [];
  private rowsWritten = 0;
  private closed = false;

  private constructor(
    private readonly handle: fs.FileHandle,
    private readonly currency: CurrencyCode,
    private readonly rowsPerFlush: number,
  ) {}

  static async open(path: string, currency: CurrencyCode, rowsPerFlush: number): Promise<StatementWriter> {
    const handle = await fs.open(path, 'wx', 0o640);
    const writer = new StatementWriter(handle, currency, rowsPerFlush);
    writer.buffer.push(toCsvRow(HEADER));
    return writer;
  }

  async writeLines(lines: Iterable<StatementLine>): Promise<void> {
    if (this.closed) {
      throw new Error('statement writer is already closed');
    }
    for (const line of lines) {
      this.buffer.push(
        toCsvRow([
          line.postedOn,
          line.description,
          formatMinor(line.debitMinor, this.currency),
          formatMinor(line.creditMinor, this.currency),
        ]),
      );
      if (this.buffer.length >= this.rowsPerFlush) {
        await this.flush();
      }
    }
  }

  async flush(): Promise<void> {
    if (this.buffer.length === 0) {
      return;
    }
    const chunk = this.buffer.join('');
    this.rowsWritten += this.buffer.length;
    this.buffer = [];
    await this.handle.write(chunk);
  }

  /** Flushes buffered rows and fsyncs before closing; safe to call more than once. */
  async close(): Promise<number> {
    if (this.closed) {
      return this.rowsWritten;
    }
    this.closed = true;
    try {
      await this.flush();
      await this.handle.sync();
    } finally {
      await this.handle.close();
    }
    return this.rowsWritten;
  }

  /** Closes the handle without flushing, for an export that is being abandoned. */
  async discard(): Promise<void> {
    if (this.closed) {
      return;
    }
    this.closed = true;
    this.buffer = [];
    await this.handle.close();
  }
}
