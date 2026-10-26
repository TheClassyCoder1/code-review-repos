'use strict';

const { accruals } = require('./repaymentStores');
const { round2 } = require('../utils/keyedLock');

/**
 * Repository for daily interest and late-fee accruals. The accrual service is
 * the only module that calls into this repository.
 */

const MS_PER_DAY = 24 * 60 * 60 * 1000;

function daysBetween(fromIso, toIso) {
  const from = Date.parse(`${fromIso}T00:00:00Z`);
  const to = Date.parse(`${toIso}T00:00:00Z`);
  if (!Number.isFinite(from) || !Number.isFinite(to) || to <= from) {
    return 0;
  }
  return Math.round((to - from) / MS_PER_DAY);
}

async function getAccrual(loanId) {
  return accruals.get(loanId);
}

/**
 * Accrue interest from the last accrual date up to asOf (exclusive).
 * Returns the updated accrual row, or null when the loan has no accrual row.
 */
async function applyDailyAccrual(loanId, asOf, principal) {
  const row = await accruals.get(loanId);
  if (!row) {
    return null;
  }
  const days = daysBetween(row.lastAccruedOn, asOf);
  if (days === 0) {
    return row;
  }
  const interest = round2(principal * row.dailyRate * days);
  row.accruedInterest = round2(row.accruedInterest + interest);
  row.lastAccruedOn = asOf;
  await accruals.set(loanId, row);
  return row;
}

/**
 * Charge a late fee against the accrual row.
 */
async function chargeLateFee(loanId, amount) {
  const row = await accruals.get(loanId);
  if (!row) {
    return null;
  }
  row.lateFees = round2(row.lateFees + amount);
  await accruals.set(loanId, row);
  return row;
}

/**
 * Waive some or all of the outstanding late fees.
 */
async function waiveLateFees(loanId, amount) {
  const row = await accruals.get(loanId);
  if (!row) {
    return null;
  }
  row.lateFees = round2(Math.max(0, row.lateFees - amount));
  await accruals.set(loanId, row);
  return row;
}

module.exports = { getAccrual, applyDailyAccrual, chargeLateFee, waiveLateFees, daysBetween };
