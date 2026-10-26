'use strict';

const { settlements } = require('../stores/repaymentStores');
const { settleEntries } = require('./batchSettlement');
const { logger } = require('../../utils/logger');

/**
 * Drives settlement of a batch end-to-end. A batch is only marked SETTLED when
 * every entry settled; any failure moves the batch to NEEDS_REVIEW and the run
 * rejects so the scheduler alerts and ops reconcile the leftovers.
 */

class SettlementIncompleteError extends Error {
  constructor(batchId, failures) {
    super(`settlement batch ${batchId} has ${failures.length} unsettled entries`);
    this.name = 'SettlementIncompleteError';
    this.status = 409;
    this.failures = failures;
  }
}

async function runSettlement(batchId, paymentRefs, bankLines) {
  const existing = await settlements.get(batchId);
  if (existing && existing.status === 'SETTLED') {
    return existing;
  }

  const { settled, failures } = await settleEntries(paymentRefs, bankLines);

  if (failures.length > 0) {
    await settlements.set(batchId, {
      batchId,
      status: 'NEEDS_REVIEW',
      settled,
      failures,
      updatedAt: Date.now(),
    });
    logger.error({ batchId, failed: failures.length }, 'part3.settlement.incomplete');
    throw new SettlementIncompleteError(batchId, failures);
  }

  const record = { batchId, status: 'SETTLED', settled, failures: [], updatedAt: Date.now() };
  await settlements.set(batchId, record);
  logger.info({ batchId, settled: settled.length }, 'part3.settlement.complete');
  return record;
}

module.exports = { runSettlement, SettlementIncompleteError };
