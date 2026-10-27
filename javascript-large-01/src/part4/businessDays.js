'use strict';

/**
 * Business-day arithmetic for installment due dates. A due date that lands on
 * a weekend or a bank holiday rolls forward to the next business day, which is
 * what the servicing agreement promises borrowers.
 *
 * All dates are handled as UTC calendar days ("YYYY-MM-DD") to avoid local
 * timezone shifts moving a due date across midnight.
 */

const DAY_MS = 24 * 60 * 60 * 1000;

// Fixed-date bank holidays observed by the servicing calendar (MM-DD).
const FIXED_HOLIDAYS = new Set(['01-01', '07-04', '11-11', '12-25']);

function toIsoDay(date) {
  return date.toISOString().slice(0, 10);
}

function parseIsoDay(day) {
  const date = new Date(`${day}T00:00:00Z`);
  if (Number.isNaN(date.getTime()) || toIsoDay(date) !== day) {
    throw new RangeError(`not a valid calendar day: ${day}`);
  }
  return date;
}

function isBusinessDay(date, extraHolidays) {
  const weekday = date.getUTCDay();
  if (weekday === 0 || weekday === 6) {
    return false;
  }
  const iso = toIsoDay(date);
  if (FIXED_HOLIDAYS.has(iso.slice(5))) {
    return false;
  }
  return !(extraHolidays && extraHolidays.has(iso));
}

/**
 * Return the given day if it is a business day, otherwise the next business
 * day after it. `extraHolidays` is an optional Set of ISO days for movable
 * holidays loaded from the servicing calendar. The search is bounded so a
 * misconfigured holiday set cannot loop forever.
 */
function nextBusinessDay(day, extraHolidays) {
  let cursor = parseIsoDay(day);
  for (let i = 0; i < 14; i += 1) {
    if (isBusinessDay(cursor, extraHolidays)) {
      return toIsoDay(cursor);
    }
    cursor = new Date(cursor.getTime() + DAY_MS);
  }
  throw new RangeError(`no business day within two weeks of ${day}`);
}

/**
 * Add whole calendar months to a day, clamping to the last day of the target
 * month (Jan 31 + 1 month = Feb 28/29).
 */
function addMonths(day, months) {
  const start = parseIsoDay(day);
  const year = start.getUTCFullYear();
  const month = start.getUTCMonth() + months;
  const lastDay = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  const target = new Date(Date.UTC(year, month, Math.min(start.getUTCDate(), lastDay)));
  return toIsoDay(target);
}

/**
 * Whole days between two ISO days (b - a). Negative when b is before a.
 */
function daysBetween(a, b) {
  return Math.round((parseIsoDay(b).getTime() - parseIsoDay(a).getTime()) / DAY_MS);
}

module.exports = { nextBusinessDay, addMonths, daysBetween, parseIsoDay, toIsoDay, isBusinessDay };
