'use strict';

const { deliveries } = require('../stores/repaymentStores');

/**
 * Tracks delivery attempts of statements and payment reminders per document so
 * the portal can show "sent by email on ..." and ops can find documents that
 * never reached the borrower.
 */

const MAX_ATTEMPTS_KEPT = 50;

function emptyRecord(documentId) {
  return { documentId, attempts: [], failures: 0, deliveredVia: [] };
}

/**
 * Append one delivery attempt to a document's record.
 */
async function recordAttempt(documentId, channel, outcome, messageId) {
  const record = (await deliveries.get(documentId)) || emptyRecord(documentId);
  record.attempts.push({ channel, outcome, messageId: messageId || null, at: new Date().toISOString() });
  if (record.attempts.length > MAX_ATTEMPTS_KEPT) {
    record.attempts.splice(0, record.attempts.length - MAX_ATTEMPTS_KEPT);
  }
  if (outcome === 'DELIVERED') {
    if (!record.deliveredVia.includes(channel)) {
      record.deliveredVia.push(channel);
    }
  } else {
    record.failures += 1;
  }
  await deliveries.set(documentId, record);
  return record;
}

async function getDeliveryRecord(documentId) {
  return (await deliveries.get(documentId)) || emptyRecord(documentId);
}

module.exports = { recordAttempt, getDeliveryRecord };
