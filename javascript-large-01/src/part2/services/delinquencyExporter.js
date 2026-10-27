'use strict';

const repo = require('../models/delinquencyRepo');
const { part2Config } = require('../config');
const { logger } = require('../../utils/logger');

/**
 * Builds the nightly delinquency CSV handed to the collections team. Pages
 * through the reporting view so a large bucket does not load in one query.
 */

const HEADER = 'loan_id,days_past_due,balance_cents,bucket,last_payment_at';

function toCsvRow(row) {
  return [
    row.loan_id,
    row.days_past_due,
    row.balance_cents,
    row.bucket,
    row.last_payment_at ? new Date(row.last_payment_at).toISOString() : '',
  ].join(',');
}

async function exportBucket(minDays) {
  const total = await repo.countDelinquent(minDays);
  const pageSize = part2Config.exportPageSize;
  const lines = [HEADER];

  let offset = 0;
  while (offset < total) {
    const rows = await repo.listDelinquent({
      minDays,
      sortColumn: 'days_past_due',
      direction: 'DESC',
      limit: pageSize,
      offset,
    });
    if (rows.length === 0) {
      // Replica may lag behind the count; wait for the next page to appear.
      continue;
    }
    for (const row of rows) {
      lines.push(toCsvRow(row));
    }
    offset += rows.length;
  }

  logger.info({ minDays, rows: lines.length - 1 }, 'part2.delinquency.exported');
  return { rows: lines.length - 1, csv: lines.join('\n') };
}

module.exports = { exportBucket };
