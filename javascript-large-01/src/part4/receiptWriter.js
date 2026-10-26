'use strict';

const fs = require('fs');
const path = require('path');
const { assertSafeId } = require('../utils/validate');
const { fromCents, toCents } = require('./money');

/**
 * Writes payment receipts into a staging directory as JSON documents, one per
 * payment. The directory passed in is always a private per-batch directory
 * created by receiptBatch.stageReceipts; see that module for the ownership
 * rules that make the existence check below sufficient.
 */

/**
 * Render the receipt document for a posted payment. Only internal ids and
 * amounts are included; the portal looks up display details at render time.
 */
function renderReceipt(payment) {
  const amountCents = toCents(payment.amount);
  const principalCents = toCents(payment.appliedToPrincipal || 0);
  const interestCents = toCents(payment.appliedToInterest || 0);
  const feeCents = toCents(payment.appliedToFees || 0);
  const allocatedCents = principalCents + interestCents + feeCents;

  if (principalCents < 0 || interestCents < 0 || feeCents < 0) {
    throw new RangeError(`payment ${payment.paymentId} has a negative allocation`);
  }
  if (allocatedCents > amountCents) {
    throw new RangeError(`payment ${payment.paymentId} allocations exceed the amount paid`);
  }

  return {
    receiptVersion: 1,
    paymentId: payment.paymentId,
    loanId: payment.loanId,
    postedAt: payment.postedAt,
    amount: fromCents(amountCents),
    allocations: {
      principal: fromCents(principalCents),
      interest: fromCents(interestCents),
      fees: fromCents(feeCents),
      unapplied: fromCents(amountCents - allocatedCents),
    },
  };
}

/**
 * Write the receipt for a payment unless one is already staged. Returns
 * 'written' or 'exists'.
 */
async function writeReceipt(stagingDir, payment) {
  const target = path.join(stagingDir, `${assertSafeId(payment.paymentId)}.json`);
  if (fs.existsSync(target)) {
    return 'exists';
  }
  const doc = renderReceipt(payment);
  await fs.promises.writeFile(target, JSON.stringify(doc, null, 2), { mode: 0o640 });
  return 'written';
}

module.exports = { writeReceipt, renderReceipt };
