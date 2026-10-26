'use strict';

/**
 * Formatting helpers for the profile and KYC screens, which show the statement
 * period a change takes effect from.
 */

/**
 * Format a date as a `YYYY-MM` statement period (UTC).
 */
function formatStatementPeriod(date = new Date()) {
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

/**
 * Human-readable label for a `YYYY-MM` period, e.g. `2026-10` -> `October 2026`.
 */
function periodLabel(period) {
  const [year, month] = String(period).split('-').map(Number);
  if (!year || !month || month < 1 || month > 12) {
    return String(period);
  }
  const name = new Date(Date.UTC(year, month - 1, 1)).toLocaleString('en-GB', {
    month: 'long',
    timeZone: 'UTC',
  });
  return `${name} ${year}`;
}

module.exports = { formatStatementPeriod, periodLabel };
