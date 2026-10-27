'use strict';

/**
 * Payment reminder cadence. A statement with an amount due gets a small number
 * of reminders before and after its due date. Offsets are expressed in whole
 * days relative to the due date: negative offsets are "before due", positive
 * offsets are "past due".
 */

const DAY_MS = 24 * 60 * 60 * 1000;
const DEFAULT_OFFSETS = Object.freeze([-5, -1, 3, 10]);
const MAX_REMINDERS = 6;

/**
 * Whole days between the due date and `nowMs`, counting only completed days.
 * Returns 0 while the statement is not yet past due.
 */
function daysPastDue(dueDateMs, nowMs) {
  if (!Number.isFinite(dueDateMs) || !Number.isFinite(nowMs)) {
    throw new TypeError('dueDateMs and nowMs must be finite numbers');
  }
  if (nowMs <= dueDateMs) {
    return 0;
  }
  return Math.floor((nowMs - dueDateMs) / DAY_MS);
}

/**
 * Build the list of reminder timestamps that are still in the future.
 *
 * Offsets are de-duplicated, sorted ascending, and capped at MAX_REMINDERS so a
 * misconfigured cadence cannot flood a customer. Reminders that would fire at
 * or before `nowMs` are dropped; the dispatcher never back-fills them.
 */
function buildReminderSchedule(dueDateMs, nowMs, offsetsDays = DEFAULT_OFFSETS) {
  if (!Array.isArray(offsetsDays)) {
    throw new TypeError('offsetsDays must be an array');
  }
  const offsets = [...new Set(offsetsDays)]
    .filter((d) => Number.isInteger(d))
    .sort((a, b) => a - b)
    .slice(0, MAX_REMINDERS);

  const schedule = [];
  for (const offset of offsets) {
    const at = dueDateMs + offset * DAY_MS;
    if (at > nowMs) {
      schedule.push({
        at,
        offsetDays: offset,
        kind: offset < 0 ? 'upcoming' : offset === 0 ? 'due-today' : 'overdue',
      });
    }
  }
  return schedule;
}

/**
 * Pick the reminder template for a statement given how far past due it is.
 */
function reminderTemplateFor(dueDateMs, nowMs) {
  const late = daysPastDue(dueDateMs, nowMs);
  if (late === 0) {
    return nowMs < dueDateMs ? 'reminder-upcoming' : 'reminder-due-today';
  }
  if (late <= 14) {
    return 'reminder-overdue';
  }
  return 'reminder-collections-handoff';
}

module.exports = {
  daysPastDue,
  buildReminderSchedule,
  reminderTemplateFor,
  DEFAULT_OFFSETS,
  MAX_REMINDERS,
};
