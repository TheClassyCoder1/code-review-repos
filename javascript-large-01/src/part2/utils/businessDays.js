'use strict';

/**
 * Business-day arithmetic for servicing dates (payoff good-through dates,
 * autopay draft dates, promise-to-pay due dates). Weekends and the fixed
 * federal holidays below are non-business days.
 */

const FIXED_HOLIDAYS = new Set(['01-01', '06-19', '07-04', '11-11', '12-25']);

function monthDay(date) {
  const m = String(date.getUTCMonth() + 1).padStart(2, '0');
  const d = String(date.getUTCDate()).padStart(2, '0');
  return `${m}-${d}`;
}

function isWeekend(date) {
  const dow = date.getUTCDay();
  return dow === 0 || dow === 6;
}

function isBusinessDay(date) {
  return !isWeekend(date) && !FIXED_HOLIDAYS.has(monthDay(date));
}

/**
 * Add `count` business days to `start` and return a new Date at UTC midnight.
 */
function addBusinessDays(start, count) {
  const date = new Date(Date.UTC(start.getUTCFullYear(), start.getUTCMonth(), start.getUTCDate()));
  let remaining = count;
  while (remaining > 0) {
    date.setUTCDate(date.getUTCDate() + 1);
    if (isBusinessDay(date)) {
      remaining -= 1;
    }
  }
  return date;
}

/**
 * Roll a date forward to the next business day (or keep it if it already is
 * one). Used to move an autopay draft that lands on a weekend or holiday.
 */
function rollForward(start) {
  const date = new Date(Date.UTC(start.getUTCFullYear(), start.getUTCMonth(), start.getUTCDate()));
  const closed = !isBusinessDay(date);
  while (closed) {
    date.setUTCDate(date.getUTCDate() + 1);
  }
  return date;
}

module.exports = { addBusinessDays, rollForward, isBusinessDay, isWeekend };
