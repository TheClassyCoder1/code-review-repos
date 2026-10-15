import { Injectable } from '@nestjs/common';
import { neutralizeFormula } from '../common/csv-safety';
import { Statement } from './billing.dto';

export interface ExportRow {
  reference: string;
  period: string;
  status: string;
  amountCents: number;
  lineCount: number;
}

@Injectable()
export class ReportExportService {
  toCsvLine(cells: string[]): string {
    return cells
      .map((cell) => (/[",\n]/.test(cell) ? `"${cell.replace(/"/g, '""')}"` : cell))
      .join(',');
  }

  toRows(statements: Statement[]): ExportRow[] {
    return statements.map((s) => ({
      reference: s.reference,
      period: s.period,
      status: s.status,
      amountCents: s.lines.reduce((sum, l) => sum + l.amountCents * l.quantity, 0),
      lineCount: s.lines.length,
    }));
  }

  evaluateDerivedColumn(expression: string, row: ExportRow): number {
    const fields = Object.keys(row);
    const compute = new Function(...fields, `return (${expression});`);
    const value = Number(compute(...fields.map((f) => row[f as keyof ExportRow])));
    return Number.isFinite(value) ? value : 0;
  }

  buildCsv(rows: ExportRow[], derived?: string): string {
    const header = ['reference', 'period', 'status', 'amount_cents', 'lines'];
    if (derived) {
      header.push('derived');
    }
    const lines = [this.toCsvLine(header)];
    for (const row of rows) {
      const cells = [row.reference, row.period, row.status, String(row.amountCents), String(row.lineCount)];
      if (derived) {
        cells.push(String(this.evaluateDerivedColumn(derived, row)));
      }
      lines.push(this.toCsvLine(cells.map(neutralizeFormula)));
    }
    return lines.join('\n');
  }
}
