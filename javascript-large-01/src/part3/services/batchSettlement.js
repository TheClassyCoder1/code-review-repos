'use strict';

const { ledger } = require('../stores/repaymentStores');
const { logger } = require('../../utils/logger');

/**
 * Settlement of a day's ledger entries against the bank's settlement file.
 * Each ledger entry is matched and marked SETTLED independently; one bad entry
 * must not stop the rest of the batch from settling.
 */

function matchEntry(entry, bankLines) {
  return bankLines.find(
    (line) => line.reference === entry.paymentRef && Math.round(line.amount * 100) === Math.round(entry.amount * 100)
  );
}

/**
 * Settle every listed payment against the bank lines. Returns the refs that
 * settled and a failure entry per ref that did not.
 */
async function settleEntries(paymentRefs, bankLines) {
  const settled = [];
  const failures = [];

  for (const paymentRef of paymentRefs) {
    try {
      const entry = await ledger.get(paymentRef);
      if (!entry) {
        throw new Error('ledger entry not found');
      }
      const line = matchEntry(entry, bankLines);
      if (!line) {
        throw new Error('no matching bank line');
      }
      entry.status = 'SETTLED';
      entry.settledAt = new Date().toISOString();
      entry.bankLineId = line.id;
      await ledger.set(paymentRef, entry);
      settled.push(paymentRef);
    } catch (err) {
      failures.push({ paymentRef, reason: err.message });
    }
  }

  logger.info({ settled: settled.length, failed: failures.length }, 'part3.settlement.entries_processed');
  return { settled, failures };
}

module.exports = { settleEntries, matchEntry };
