import { IsoDate } from '../domain/types';

const MS_PER_DAY = 86_400_000;

export function parseIsoDate(value: IsoDate): Date {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) {
    throw new RangeError(`not an ISO calendar date: ${value}`);
  }
  const [, y, m, d] = match;
  const date = new Date(Date.UTC(Number(y), Number(m) - 1, Number(d)));
  if (date.getUTCMonth() !== Number(m) - 1) {
    throw new RangeError(`invalid calendar date: ${value}`);
  }
  return date;
}

export function toIsoDate(date: Date): IsoDate {
  return date.toISOString().slice(0, 10);
}

export function daysBetween(from: IsoDate, to: IsoDate): number {
  return Math.round((parseIsoDate(to).getTime() - parseIsoDate(from).getTime()) / MS_PER_DAY);
}

export function addDays(date: IsoDate, days: number): IsoDate {
  return toIsoDate(new Date(parseIsoDate(date).getTime() + days * MS_PER_DAY));
}

/**
 * Bank holidays and weekend handling for ACH settlement dates.
 * Holidays are injected so tests and different jurisdictions can supply their own list.
 */
export class BusinessCalendar {
  private readonly holidays: ReadonlySet<IsoDate>;

  constructor(holidays: Iterable<IsoDate>) {
    this.holidays = new Set(holidays);
  }

  isBusinessDay(date: IsoDate): boolean {
    const dow = parseIsoDate(date).getUTCDay();
    return dow !== 0 && dow !== 6 && !this.holidays.has(date);
  }

  /** Moves forward `count` business days; `count = 0` rolls a non-business day forward. */
  addBusinessDays(start: IsoDate, count: number): IsoDate {
    if (!Number.isInteger(count) || count < 0) {
      throw new RangeError(`count must be a non-negative integer, got ${count}`);
    }
    let cursor = start;
    while (!this.isBusinessDay(cursor)) {
      cursor = addDays(cursor, 1);
    }
    let remaining = count;
    while (remaining > 0) {
      cursor = addDays(cursor, 1);
      if (this.isBusinessDay(cursor)) {
        remaining--;
      }
    }
    return cursor;
  }

  /**
   * Due date for installment `sequence` (1-based) anchored on the first due date.
   * Short months clamp to their last day (Jan 31 -> Feb 28/29 -> Mar 31) instead of
   * drifting, and the result is never shifted: due dates are contractual, only
   * settlement dates roll to business days.
   */
  nthDueDate(firstDueDate: IsoDate, sequence: number): IsoDate {
    if (!Number.isInteger(sequence) || sequence < 1) {
      throw new RangeError(`sequence must be >= 1, got ${sequence}`);
    }
    const anchor = parseIsoDate(firstDueDate);
    const anchorDay = anchor.getUTCDate();
    const monthIndex = anchor.getUTCMonth() + (sequence - 1);
    const year = anchor.getUTCFullYear() + Math.floor(monthIndex / 12);
    const month = ((monthIndex % 12) + 12) % 12;
    const lastDayOfMonth = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
    const day = Math.min(anchorDay, lastDayOfMonth);
    return toIsoDate(new Date(Date.UTC(year, month, day)));
  }

  /** Settlement date for a payment due on `dueDate`: next business day if it falls on a closure. */
  settlementDateFor(dueDate: IsoDate): IsoDate {
    return this.addBusinessDays(dueDate, 0);
  }
}
