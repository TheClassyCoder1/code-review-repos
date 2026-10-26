'use strict';

const sql = require('../db/sqlClient');

/**
 * Read-side queries over the delinquency reporting view on the replica.
 */

/**
 * Page through delinquent accounts in a bucket.
 *
 * @param {object} q
 * @param {number} q.minDays   - lower bound on days past due
 * @param {string} q.sortColumn - column to order by (resolved by the caller)
 * @param {string} q.direction  - ASC or DESC (resolved by the caller)
 * @param {number} q.limit
 * @param {number} q.offset
 */
async function listDelinquent({ minDays, sortColumn, direction, limit, offset }) {
  return sql.query(
    `SELECT loan_id, days_past_due, balance_cents, bucket, last_payment_at
       FROM reporting.delinquent_accounts
      WHERE days_past_due >= $1
      ORDER BY ${sortColumn} ${direction}, loan_id ASC
      LIMIT $2 OFFSET $3`,
    [minDays, limit, offset]
  );
}

async function countDelinquent(minDays) {
  const rows = await sql.query(
    'SELECT count(*)::int AS total FROM reporting.delinquent_accounts WHERE days_past_due >= $1',
    [minDays]
  );
  return rows[0] ? rows[0].total : 0;
}

module.exports = { listDelinquent, countDelinquent };
