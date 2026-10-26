import { promises as fs } from 'fs';
import * as path from 'path';
import { ExportSettings } from '../config/settings';
import { CurrencyCode, StatementLine, StatementSummary } from '../domain/types';
import { Logger } from '../util/logger';
import { StatementWriter } from './statementWriter';

export interface StatementSource {
  openingBalance(loanId: number, periodStart: string): Promise<number>;
  lines(loanId: number, periodStart: string, periodEnd: string): Promise<StatementLine[]>;
}

/**
 * Totals a statement period. Lines must fall inside [periodStart, periodEnd];
 * a line outside the window means the source query is wrong, so it fails
 * loudly instead of silently producing a balance that does not reconcile.
 */
export function summarizeStatement(
  loanId: number,
  openingBalanceMinor: number,
  lines: StatementLine[],
  periodStart: string,
  periodEnd: string,
): StatementSummary {
  let totalDebitsMinor = 0;
  let totalCreditsMinor = 0;
  for (const line of lines) {
    if (line.postedOn < periodStart || line.postedOn > periodEnd) {
      throw new RangeError(`statement line dated ${line.postedOn} is outside ${periodStart}..${periodEnd}`);
    }
    if (line.debitMinor < 0 || line.creditMinor < 0) {
      throw new RangeError('statement lines carry unsigned debit and credit amounts');
    }
    totalDebitsMinor += line.debitMinor;
    totalCreditsMinor += line.creditMinor;
  }
  return {
    loanId,
    openingBalanceMinor,
    totalDebitsMinor,
    totalCreditsMinor,
    closingBalanceMinor: openingBalanceMinor + totalDebitsMinor - totalCreditsMinor,
    lineCount: lines.length,
  };
}

/**
 * Produces one CSV statement per loan. The writer is always closed before
 * `exportLoan` returns: committed on success, discarded and the partial file
 * removed on failure.
 */
export class StatementExportJob {
  constructor(
    private readonly source: StatementSource,
    private readonly settings: ExportSettings,
    private readonly logger: Logger,
  ) {}

  async exportLoan(
    loanId: number,
    currency: CurrencyCode,
    periodStart: string,
    periodEnd: string,
  ): Promise<StatementSummary> {
    const opening = await this.source.openingBalance(loanId, periodStart);
    const lines = await this.source.lines(loanId, periodStart, periodEnd);
    const summary = summarizeStatement(loanId, opening, lines, periodStart, periodEnd);

    const target = path.join(this.settings.outputDir, `statement-${loanId}-${periodEnd}.csv`);
    const writer = await StatementWriter.open(target, currency, this.settings.rowsPerFlush);
    let committed = false;
    try {
      await writer.writeLines(lines);
      const rows = await writer.close();
      committed = true;
      this.logger.log('info', 'statement.exported', { loanId, rows, periodEnd });
    } finally {
      if (!committed) {
        await writer.discard();
        await fs.rm(target, { force: true });
        this.logger.log('warn', 'statement.export_aborted', { loanId, periodEnd });
      }
    }
    return summary;
  }

  async exportMany(
    loanIds: number[],
    currency: CurrencyCode,
    periodStart: string,
    periodEnd: string,
  ): Promise<{ exported: number; failed: number[] }> {
    const failed: number[] = [];
    let exported = 0;
    for (const loanId of loanIds) {
      try {
        await this.exportLoan(loanId, currency, periodStart, periodEnd);
        exported += 1;
      } catch (error) {
        failed.push(loanId);
        this.logger.log('error', 'statement.export_failed', { loanId, error: (error as Error).name });
      }
    }
    return { exported, failed };
  }
}
