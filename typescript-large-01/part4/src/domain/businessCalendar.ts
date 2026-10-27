/**
 * Date-only arithmetic for due dates and settlement windows.
 *
 * All values are ISO calendar dates ("YYYY-MM-DD") interpreted in UTC so that
 * the worker's host timezone can never shift a due date by a day.
 */
const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;
const MS_PER_DAY = 86_400_000;

export function parseIsoDate(value: string): Date {
  const match = ISO_DATE.exec(value);
  if (!match) {
    throw new RangeError(`expected YYYY-MM-DD, got "${value}"`);
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
    throw new RangeError(`"${value}" is not a real calendar date`);
  }
  return date;
}

export function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function daysBetween(fromIso: string, toIso: string): number {
  return Math.round((parseIsoDate(toIso).getTime() - parseIsoDate(fromIso).getTime()) / MS_PER_DAY);
}

export class BusinessCalendar {
  private readonly holidays: ReadonlySet<string>;

  constructor(holidays: Iterable<string>) {
    const normalized = new Set<string>();
    for (const holiday of holidays) {
      normalized.add(toIsoDate(parseIsoDate(holiday)));
    }
    this.holidays = normalized;
  }

  isBusinessDay(isoDate: string): boolean {
    const weekday = parseIsoDate(isoDate).getUTCDay();
    return weekday !== 0 && weekday !== 6 && !this.holidays.has(isoDate);
  }

  /**
   * Moves `days` business days forward (or backward when negative) from
   * `startIso`. The start date itself is never counted, so adding zero days to
   * a holiday returns the holiday unchanged.
   */
  addBusinessDays(startIso: string, days: number): string {
    if (!Number.isInteger(days)) {
      throw new RangeError('days must be an integer');
    }
    const step = days < 0 ? -1 : 1;
    let remaining = Math.abs(days);
    let cursor = parseIsoDate(startIso).getTime();
    let guard = 0;

    while (remaining > 0) {
      cursor += step * MS_PER_DAY;
      if (this.isBusinessDay(toIsoDate(new Date(cursor)))) {
        remaining -= 1;
      }
      guard += 1;
      if (guard > 3_660) {
        throw new Error('business-day search exceeded ten years; check the holiday list');
      }
    }
    return toIsoDate(new Date(cursor));
  }

  /** Rolls a non-business date forward to the next business day (modified-following). */
  rollForward(isoDate: string): string {
    if (this.isBusinessDay(isoDate)) {
      return isoDate;
    }
    const next = this.addBusinessDays(isoDate, 1);
    if (next.slice(0, 7) !== isoDate.slice(0, 7)) {
      return this.addBusinessDays(isoDate, -1);
    }
    return next;
  }
}

/**
 * Returns the due date `monthsAhead` months after `anchorIso`, keeping the
 * anchor's day-of-month where possible and clamping to the last day of shorter
 * months (an anchor of Jan 31 gives Feb 28/29, Mar 31, Apr 30, ...).
 */
export function nthMonthlyDueDate(anchorIso: string, monthsAhead: number): string {
  if (!Number.isInteger(monthsAhead) || monthsAhead < 0) {
    throw new RangeError('monthsAhead must be a non-negative integer');
  }
  const anchor = parseIsoDate(anchorIso);
  const anchorDay = anchor.getUTCDate();
  const totalMonths = anchor.getUTCMonth() + monthsAhead;
  const year = anchor.getUTCFullYear() + Math.floor(totalMonths / 12);
  const month = totalMonths % 12;

  const lastDayOfTarget = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  const day = Math.min(anchorDay, lastDayOfTarget);
  return toIsoDate(new Date(Date.UTC(year, month, day)));
}
