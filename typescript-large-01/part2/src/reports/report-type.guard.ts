import { HttpError } from '../http/respond';

export const REPORT_TYPES = ['amortization', 'payoff-quote', 'interest-paid', 'escrow-analysis'] as const;
export type ReportType = (typeof REPORT_TYPES)[number];

export interface ReportRequest {
  type: ReportType;
  loanId: string;
  year: string;
}

const LOAN_ID = /^[0-9]{1,12}$/;
const YEAR = /^20[0-9]{2}$/;

/**
 * Validates report parameters at the HTTP boundary. Every field is either an
 * allowlisted literal or a short digit string; the report renderer relies on
 * receiving nothing else.
 */
export function parseReportRequest(params: Record<string, string>, query: URLSearchParams): ReportRequest {
  const type = params.reportType;
  if (!(REPORT_TYPES as readonly string[]).includes(type)) {
    throw new HttpError(400, 'unknown report type');
  }
  const loanId = params.loanId;
  if (!LOAN_ID.test(loanId)) {
    throw new HttpError(400, 'loanId must be numeric');
  }
  const year = query.get('year') ?? String(new Date().getUTCFullYear());
  if (!YEAR.test(year)) {
    throw new HttpError(400, 'year must be a four-digit year');
  }
  return { type: type as ReportType, loanId, year };
}
