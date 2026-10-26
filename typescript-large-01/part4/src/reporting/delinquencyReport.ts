import { daysBetween } from '../domain/businessCalendar';
import { MinorUnits, RepaymentEntry } from '../domain/types';

export type DelinquencyBucket = 'CURRENT' | 'DPD_1_29' | 'DPD_30_59' | 'DPD_60_89' | 'DPD_90_PLUS';

export interface LoanDelinquency {
  loanId: number;
  daysPastDue: number;
  bucket: DelinquencyBucket;
  overdueMinor: MinorUnits;
}

export interface PortfolioBucketTotals {
  bucket: DelinquencyBucket;
  loanCount: number;
  overdueMinor: MinorUnits;
  shareOfOverdue: number;
}

const BUCKET_ORDER: readonly DelinquencyBucket[] = ['CURRENT', 'DPD_1_29', 'DPD_30_59', 'DPD_60_89', 'DPD_90_PLUS'];

export function bucketFor(daysPastDue: number): DelinquencyBucket {
  if (daysPastDue <= 0) return 'CURRENT';
  if (daysPastDue < 30) return 'DPD_1_29';
  if (daysPastDue < 60) return 'DPD_30_59';
  if (daysPastDue < 90) return 'DPD_60_89';
  return 'DPD_90_PLUS';
}

/**
 * Delinquency for one loan as of `asOfIso`. Days past due are measured from
 * the oldest installment that is due and not fully paid; installments not yet
 * due never count toward the overdue amount.
 */
export function assessLoanDelinquency(loanId: number, entries: RepaymentEntry[], asOfIso: string): LoanDelinquency {
  let oldestUnpaidDue: string | null = null;
  let overdueMinor = 0;

  for (const entry of entries) {
    if (entry.loanId !== loanId) {
      throw new Error(`repayment entry for loan ${entry.loanId} passed to loan ${loanId}`);
    }
    const outstanding = entry.amountDueMinor - entry.amountPaidMinor;
    if (outstanding <= 0 || entry.dueDate >= asOfIso) {
      continue;
    }
    overdueMinor += outstanding;
    if (oldestUnpaidDue === null || entry.dueDate < oldestUnpaidDue) {
      oldestUnpaidDue = entry.dueDate;
    }
  }

  const daysPastDue = oldestUnpaidDue === null ? 0 : daysBetween(oldestUnpaidDue, asOfIso);
  return { loanId, daysPastDue, bucket: bucketFor(daysPastDue), overdueMinor };
}

/**
 * Rolls loan-level delinquency up into per-bucket totals. Every bucket is
 * present in the output (in severity order) even when empty, so dashboards
 * can render a fixed set of columns. Shares are rounded to four decimals.
 */
export function aggregatePortfolio(loans: LoanDelinquency[]): PortfolioBucketTotals[] {
  const totals = new Map<DelinquencyBucket, { loanCount: number; overdueMinor: number }>();
  for (const bucket of BUCKET_ORDER) {
    totals.set(bucket, { loanCount: 0, overdueMinor: 0 });
  }

  const seen = new Set<number>();
  let grandOverdue = 0;
  for (const loan of loans) {
    if (seen.has(loan.loanId)) {
      throw new Error(`loan ${loan.loanId} appears twice in the portfolio snapshot`);
    }
    seen.add(loan.loanId);
    const slot = totals.get(loan.bucket)!;
    slot.loanCount += 1;
    slot.overdueMinor += loan.overdueMinor;
    grandOverdue += loan.overdueMinor;
  }

  return BUCKET_ORDER.map((bucket) => {
    const { loanCount, overdueMinor } = totals.get(bucket)!;
    const share = grandOverdue === 0 ? 0 : overdueMinor / grandOverdue;
    return { bucket, loanCount, overdueMinor, shareOfOverdue: Math.round(share * 10_000) / 10_000 };
  });
}
