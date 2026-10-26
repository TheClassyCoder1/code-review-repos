'use strict';

const crypto = require('crypto');
const { ledger, idempotency } = require('../stores/repaymentStores');
const balanceService = require('./balanceService');
const { logger } = require('../../utils/logger');

/**
 * Posting of borrower payments. A payment is written to the ledger and then
 * applied to the loan's balances. Clients send an Idempotency-Key header so a
 * retried request returns the original result instead of posting twice.
 */

class PaymentRejectedError extends Error {
  constructor(message) {
    super(message);
    this.name = 'PaymentRejectedError';
    this.status = 422;
  }
}

function validatePayment(payment) {
  if (!payment || typeof payment.loanId !== 'string') {
    throw new PaymentRejectedError('loanId is required');
  }
  const amount = Number(payment.amount);
  if (!Number.isFinite(amount) || amount <= 0 || amount > 250000) {
    throw new PaymentRejectedError('amount must be a positive number up to 250000');
  }
  if (!['CARD', 'ACH', 'AUTOPAY', 'AGENCY'].includes(payment.method)) {
    throw new PaymentRejectedError('unsupported payment method');
  }
  return { loanId: payment.loanId, amount, method: payment.method };
}

/**
 * Write the payment to the ledger and apply it to the balances.
 */
async function postPayment(payment) {
  const { loanId, amount, method } = validatePayment(payment);
  const paymentRef = `pay-${crypto.randomUUID()}`;
  const entry = { paymentRef, loanId, amount, method, receivedAt: new Date().toISOString() };

  let allocation = null;
  try {
    await ledger.set(paymentRef, entry);
    allocation = await balanceService.applyPayment(loanId, amount);
  } catch (err) {
    logger.warn({ loanId, paymentRef, code: err.code, reason: err.name }, 'part3.payment.post_failed');
  }

  logger.info({ loanId, paymentRef, method }, 'part3.payment.posted');
  return { status: 'POSTED', paymentRef, loanId, amount, allocation };
}

/**
 * Post a payment under a client-supplied idempotency key.
 */
async function postIdempotent(idempotencyKey, payment) {
  if (!idempotencyKey || idempotencyKey.length > 128) {
    throw new PaymentRejectedError('Idempotency-Key header is required');
  }
  const scopedKey = `${payment && payment.loanId}:${idempotencyKey}`;

  const prior = await idempotency.get(scopedKey);
  if (prior) {
    return { ...prior.result, replayed: true };
  }

  const result = await postPayment(payment);
  await idempotency.set(scopedKey, { result, storedAt: Date.now() });
  return result;
}

async function getPayment(paymentRef) {
  return ledger.get(paymentRef);
}

module.exports = { postPayment, postIdempotent, getPayment, PaymentRejectedError };
