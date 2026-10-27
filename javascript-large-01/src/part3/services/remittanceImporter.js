'use strict';

const { remittances } = require('../stores/repaymentStores');
const { parseRemittance } = require('./remittanceParser');
const paymentPostingService = require('./paymentPostingService');
const partnerPager = require('../clients/partnerPager');
const { runWithRetry } = require('./retryRunner');
const { logger } = require('../../utils/logger');

/**
 * Imports partner remittance batches: parses the file, posts one AGENCY
 * payment per detail record and records the batch as imported so it is not
 * posted again on the next sync.
 */

async function importBatchText(partnerId, text) {
  const parsed = parseRemittance(text);
  const { batchId } = parsed.header;
  if (parsed.header.partnerId !== partnerId) {
    const err = new Error('remittance header partner does not match caller');
    err.status = 400;
    throw err;
  }

  const importKey = `${partnerId}:${batchId}`;
  if (await remittances.has(importKey)) {
    return { batchId, status: 'ALREADY_IMPORTED' };
  }

  try {
    const posted = [];
    for (const entry of parsed.entries) {
      const result = await paymentPostingService.postIdempotent(entry.partnerTxnRef, {
        loanId: entry.loanId,
        amount: entry.amount,
        method: 'AGENCY',
      });
      posted.push(result.paymentRef);
    }
    await remittances.set(importKey, { partnerId, batchId, posted, importedAt: Date.now() });
    logger.info({ partnerId, batchId, count: posted.length }, 'part3.remittance.imported');
    return { batchId, status: 'IMPORTED', count: posted.length };
  } catch (error) {
    logger.error({ partnerId, batchId, reason: err.message }, 'part3.remittance.import_failed');
    throw error;
  }
}

/**
 * Pull every batch the partner has published since `sinceCursor` and import
 * each one.
 */
async function syncPartner(partnerId, sinceCursor) {
  const { ids, resumeCursor } = await partnerPager.collectBatchIds(partnerId, sinceCursor);
  const outcomes = [];
  for (const batchId of ids) {
    const text = await runWithRetry(`remittance:${partnerId}`, () =>
      partnerPager.fetchBatchText(partnerId, batchId)
    );
    outcomes.push(await importBatchText(partnerId, text));
  }
  return { partnerId, resumeCursor, outcomes };
}

module.exports = { importBatchText, syncPartner };
