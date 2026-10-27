'use strict';

/**
 * Builds the summary block that sits at the top of a delivered statement and
 * the per-period rollup used by the delivery dashboard.
 */

const { isPeriod } = require('../../utils/dates');

const CREDIT_TYPES = new Set(['payment', 'refund', 'adjustment-credit']);
const DEBIT_TYPES = new Set(['disbursement', 'interest', 'fee', 'adjustment-debit']);

/**
 * Summarise a statement's transactions into debit/credit totals in cents.
 *
 * Transactions with an unknown type are counted separately rather than being
 * silently folded into either side, so the renderer can flag the statement for
 * review instead of printing a wrong closing balance.
 */
function summariseTransactions(openingBalanceCents, transactions) {
  if (!Number.isSafeInteger(openingBalanceCents)) {
    throw new RangeError('openingBalanceCents must be an integer');
  }
  const summary = {
    openingBalanceCents,
    debitsCents: 0,
    creditsCents: 0,
    transactionCount: 0,
    unclassified: [],
  };

  for (const tx of transactions || []) {
    if (!tx || !Number.isSafeInteger(tx.amountCents) || tx.amountCents < 0) {
      summary.unclassified.push(tx && tx.id ? tx.id : null);
      continue;
    }
    if (DEBIT_TYPES.has(tx.type)) {
      summary.debitsCents += tx.amountCents;
    } else if (CREDIT_TYPES.has(tx.type)) {
      summary.creditsCents += tx.amountCents;
    } else {
      summary.unclassified.push(tx.id || null);
      continue;
    }
    summary.transactionCount += 1;
  }

  summary.closingBalanceCents = openingBalanceCents + summary.debitsCents - summary.creditsCents;
  summary.needsReview = summary.unclassified.length > 0;
  return summary;
}

/**
 * Roll statements up by period for the delivery dashboard. Returns periods in
 * ascending order with counts and delivered/pending splits. Statements with a
 * malformed period are skipped and counted under `skipped`.
 */
function groupByPeriod(statements) {
  const buckets = new Map();
  let skipped = 0;

  for (const statement of statements || []) {
    if (!statement || !isPeriod(statement.period)) {
      skipped += 1;
      continue;
    }
    let bucket = buckets.get(statement.period);
    if (!bucket) {
      bucket = { period: statement.period, total: 0, delivered: 0, pending: 0 };
      buckets.set(statement.period, bucket);
    }
    bucket.total += 1;
    if (statement.deliveredAt) {
      bucket.delivered += 1;
    } else {
      bucket.pending += 1;
    }
  }

  const periods = [...buckets.values()].sort((a, b) => a.period.localeCompare(b.period));
  return { periods, skipped };
}

module.exports = { summariseTransactions, groupByPeriod, CREDIT_TYPES, DEBIT_TYPES };
