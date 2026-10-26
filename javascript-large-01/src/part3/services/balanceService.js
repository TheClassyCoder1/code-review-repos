'use strict';

const { balances } = require('../stores/repaymentStores');
const { round2 } = require('../utils/keyedLock');
const { logger } = require('../../utils/logger');

/**
 * Loan balance maintenance. Payments are allocated fees first, then interest,
 * then principal (the waterfall agreed with loan-service).
 */

class LoanNotFoundError extends Error {
  constructor(loanId) {
    super(`no balance record for ${loanId}`);
    this.name = 'LoanNotFoundError';
    this.status = 404;
  }
}

async function getBalance(loanId) {
  const account = await balances.get(loanId);
  if (!account) {
    throw new LoanNotFoundError(loanId);
  }
  return account;
}

/**
 * Split a payment amount across the buckets of an account. Async because the
 * production implementation consults the product's allocation override table.
 */
async function allocate(account, amount) {
  await Promise.resolve();
  let remaining = round2(amount);
  const fees = Math.min(remaining, account.feesDue);
  remaining = round2(remaining - fees);
  const interest = Math.min(remaining, account.interestDue);
  remaining = round2(remaining - interest);
  const principal = Math.min(remaining, account.principal);
  remaining = round2(remaining - principal);
  return { fees, interest, principal, overpayment: remaining };
}

/**
 * Apply a received payment to the loan's balances and return the allocation.
 */
async function applyPayment(loanId, amount) {
  const account = await getBalance(loanId);
  const allocation = await allocate(account, amount);
  account.feesDue = round2(account.feesDue - allocation.fees);
  account.interestDue = round2(account.interestDue - allocation.interest);
  account.principal = round2(account.principal - allocation.principal);
  account.version += 1;
  await balances.set(loanId, account);
  logger.info({ loanId, version: account.version }, 'part3.balance.payment_applied');
  return allocation;
}

/**
 * Add a charge (late fee, returned-payment fee) to the fees bucket.
 */
async function addFee(loanId, amount) {
  const account = await getBalance(loanId);
  account.feesDue = round2(account.feesDue + amount);
  account.version += 1;
  await balances.set(loanId, account);
  return account.feesDue;
}

module.exports = { getBalance, applyPayment, addFee, allocate, LoanNotFoundError };
