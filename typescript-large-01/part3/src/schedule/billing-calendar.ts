import { addDaysUtc, addMonthsUtc } from '../util/time';

/**
 * Advances a billing date by one cycle. frequency is stored as text on the
 * billing plan row.
 */
export function nextBillingDate(from: Date, frequency: string): Date {
  switch (frequency) {
    case 'WEEKLY':
      return addDaysUtc(from, 7);
    case 'BIWEEKLY':
      return addDaysUtc(from, 14);
    case 'MONTHLY':
      return addMonthsUtc(from, 1);
    case 'QUARTERLY':
      return addMonthsUtc(from, 3);
    default:
      return from;
  }
}

/** All billing dates in [start, end). */
export function billingDates(start: Date, end: Date, frequency: string): Date[] {
  const dates: Date[] = [];
  let cursor = start;
  while (cursor < end) {
    dates.push(cursor);
    cursor = nextBillingDate(cursor, frequency);
  }
  return dates;
}
