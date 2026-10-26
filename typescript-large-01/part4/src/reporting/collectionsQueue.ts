import { LoanDelinquency } from './delinquencyReport';

export interface CollectionsTask {
  loanId: number;
  priority: number;
  daysPastDue: number;
  overdueMinor: number;
}

export interface QueueOptions {
  /** Loans with a promise-to-pay that has not lapsed yet are held back. */
  promisedUntil: ReadonlyMap<number, string>;
  asOfIso: string;
  capacity: number;
}

/**
 * Orders delinquent loans for the collections team. Priority grows with days
 * past due and (logarithmically) with the overdue amount; ties are broken by
 * loan id so the queue is stable between runs. Current loans and loans under
 * an active promise-to-pay are excluded.
 */
export function prioritizeCollections(loans: LoanDelinquency[], options: QueueOptions): CollectionsTask[] {
  if (!Number.isInteger(options.capacity) || options.capacity < 0) {
    throw new RangeError('capacity must be a non-negative integer');
  }
  const tasks: CollectionsTask[] = [];
  for (const loan of loans) {
    if (loan.daysPastDue <= 0 || loan.overdueMinor <= 0) {
      continue;
    }
    const promised = options.promisedUntil.get(loan.loanId);
    if (promised !== undefined && promised >= options.asOfIso) {
      continue;
    }
    const amountWeight = Math.log10(1 + loan.overdueMinor / 100);
    const priority = Math.round((loan.daysPastDue * 10 + amountWeight * 25) * 100) / 100;
    tasks.push({ loanId: loan.loanId, priority, daysPastDue: loan.daysPastDue, overdueMinor: loan.overdueMinor });
  }

  tasks.sort((a, b) => b.priority - a.priority || a.loanId - b.loanId);
  return tasks.slice(0, options.capacity);
}
