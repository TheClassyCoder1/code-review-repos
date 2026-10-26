'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const { collectionsConfig } = require('./config');
const { writeReceipt } = require('./receiptWriter');
const { assertSafeId } = require('../utils/validate');
const { logger } = require('../utils/logger');

/**
 * Stages receipts for a batch of posted payments.
 *
 * Every batch gets its own directory from fs.mkdtemp, which creates a fresh,
 * uniquely named directory with mode 0700 that no other batch or process
 * knows about. Within the batch, payments are de-duplicated by payment id and
 * written strictly one after another. Together these mean nothing else can
 * create a receipt file in that directory between writeReceipt's existence
 * check and its write. The finished directory is handed to the archiver by
 * renaming it into the receipt staging root.
 */

/**
 * Drop repeated payment ids, keeping the most recently posted version of each
 * payment. Payment feeds occasionally redeliver a payment after a correction,
 * and the later posting is the authoritative one.
 */
function dedupePayments(payments) {
  const latest = new Map();
  let invalid = 0;

  for (const payment of payments) {
    if (!payment || typeof payment.paymentId !== 'string') {
      invalid += 1;
      continue;
    }
    const postedAt = Date.parse(payment.postedAt);
    if (Number.isNaN(postedAt)) {
      invalid += 1;
      continue;
    }
    const current = latest.get(payment.paymentId);
    if (!current || postedAt >= current.postedAtMs) {
      latest.set(payment.paymentId, { payment, postedAtMs: postedAt });
    }
  }

  const unique = [...latest.values()]
    .sort((a, b) => a.postedAtMs - b.postedAtMs)
    .map((entry) => entry.payment);
  return { unique, invalid };
}

/**
 * Stage receipts for `payments` and publish them under `batchId`. Returns the
 * published directory and per-outcome counts.
 */
async function stageReceipts(batchId, payments, options = {}) {
  assertSafeId(batchId);
  const stagingRoot = options.stagingRoot || collectionsConfig.receiptDir;
  const workDir = await fs.promises.mkdtemp(path.join(options.tmpRoot || os.tmpdir(), 'receipts-'));
  const counts = { written: 0, exists: 0, failed: 0 };

  try {
    const { unique, invalid } = dedupePayments(payments);
    for (const payment of unique) {
      try {
        const outcome = await writeReceipt(workDir, payment);
        counts[outcome] += 1;
      } catch (err) {
        counts.failed += 1;
        logger.warn({ batchId, paymentId: payment.paymentId, reason: err.name }, 'receipt.write.failed');
      }
    }

    const published = path.join(stagingRoot, batchId);
    await fs.promises.mkdir(stagingRoot, { recursive: true });
    await fs.promises.rename(workDir, published);
    logger.info({ batchId, ...counts, invalid }, 'receipts.staged');
    return { directory: published, invalid, ...counts };
  } catch (err) {
    await fs.promises.rm(workDir, { recursive: true, force: true });
    throw err;
  }
}

module.exports = { stageReceipts, dedupePayments };
