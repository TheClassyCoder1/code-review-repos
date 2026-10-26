import { createWriteStream, WriteStream } from 'fs';
import { join } from 'path';

export interface StatementLine {
  postedOn: string;
  kind: string;
  amountMinor: number;
  runningBalanceMinor: number;
  reference: string;
}

export const STATEMENT_HEADER = 'posted_on,kind,amount_minor,running_balance_minor,reference\n';

function csvField(value: string | number): string {
  const text = String(value);
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

export function toCsvRow(line: StatementLine): string {
  return [line.postedOn, line.kind, line.amountMinor, line.runningBalanceMinor, line.reference].map(csvField).join(',') + '\n';
}

export function statementPath(dir: string, accountId: string, period: string): string {
  return join(dir, `${accountId}-${period}.csv`);
}

/** Opens the statement file for an account/period and writes the CSV header. */
export function openStatementFile(dir: string, accountId: string, period: string): WriteStream {
  const out = createWriteStream(statementPath(dir, accountId, period), { flags: 'w', encoding: 'utf8' });
  out.write(STATEMENT_HEADER);
  return out;
}
