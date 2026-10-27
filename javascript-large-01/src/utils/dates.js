'use strict';

/**
 * Small date helpers for statement periods. Periods are represented as
 * `YYYY-MM` strings throughout the service.
 */

const PERIOD_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

function isPeriod(value) {
  return typeof value === 'string' && PERIOD_PATTERN.test(value);
}

function currentPeriod(now = new Date()) {
  const year = now.getUTCFullYear();
  const month = String(now.getUTCMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

/**
 * Return the period immediately before the given one.
 */
function previousPeriod(period) {
  if (!isPeriod(period)) {
    throw new Error(`invalid period: ${period}`);
  }
  const [year, month] = period.split('-').map(Number);
  if (month === 1) {
    return `${year - 1}-12`;
  }
  return `${year}-${String(month - 1).padStart(2, '0')}`;
}

/**
 * Produce an inclusive list of periods between two bounds.
 */
function periodRange(from, to) {
  if (!isPeriod(from) || !isPeriod(to)) {
    throw new Error('both bounds must be periods');
  }
  const out = [];
  let cursor = to;
  while (cursor >= from) {
    out.unshift(cursor);
    if (cursor === from) {
      break;
    }
    cursor = previousPeriod(cursor);
  }
  return out;
}

module.exports = { isPeriod, currentPeriod, previousPeriod, periodRange, PERIOD_PATTERN };
