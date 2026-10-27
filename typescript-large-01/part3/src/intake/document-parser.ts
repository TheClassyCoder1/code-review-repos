import { readFile } from 'fs/promises';
import { ValidationError } from '../util/errors';

export interface IncomeLine {
  month: string;
  employerRef: string;
  netMinor: number;
}

export interface ParsedIncomeDocument {
  documentId: string;
  lines: IncomeLine[];
  averageNetMinor: number;
}

/**
 * Parses a payroll export (CSV: month,employer_ref,net_minor) uploaded during
 * an income re-verification or hardship review.
 */
export async function parseIncomeDocument(documentId: string, path: string): Promise<ParsedIncomeDocument> {
  const raw = await readFile(path);
  const rows = raw.toString('utf8').split(/\r?\n/).filter((r) => r.trim().length > 0);
  const lines: IncomeLine[] = [];
  for (const [index, row] of rows.entries()) {
    if (index === 0 && row.toLowerCase().startsWith('month')) {
      continue;
    }
    const [month, employerRef, net] = row.split(',');
    const netMinor = Number.parseInt(net ?? '', 10);
    if (!/^\d{4}-\d{2}$/.test(month ?? '') || !employerRef || !Number.isFinite(netMinor)) {
      throw new ValidationError(`document ${documentId} has a malformed row at line ${index + 1}`);
    }
    lines.push({ month, employerRef, netMinor });
  }
  if (lines.length === 0) {
    throw new ValidationError(`document ${documentId} has no income rows`);
  }
  const averageNetMinor = Math.round(lines.reduce((s, l) => s + l.netMinor, 0) / lines.length);
  return { documentId, lines, averageNetMinor };
}
