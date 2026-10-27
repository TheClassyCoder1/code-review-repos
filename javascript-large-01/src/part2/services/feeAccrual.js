'use strict';

const loanAccounts = require('../models/loanAccounts');
const servicingRules = require('./servicingRules');
const { logger } = require('../../utils/logger');

/**
 * Late-fee assessment. Runs from the nightly job and also on demand when an
 * agent re-ages an account. Fee amounts come from the servicing rules for the
 * loan's product, capped per the rule's lateFeeCap.
 */

function productFor(loan) {
  return loan.planId.split('-')[0];
}

/**
 * Assess one late fee on a loan if it is past its grace period.
 *
 * @returns {Promise<{ loanId: string, assessed: number, fees: number }>}
 */
async function assessLateFee(loanId) {
  const loan = await loanAccounts.get(loanId);
  if (!loan) {
    throw Object.assign(new Error('no such loan'), { status: 404 });
  }
  const rule = servicingRules.forProduct(productFor(loan));
  if (!rule || loan.daysPastDue <= rule.graceDays) {
    return { loanId, assessed: 0, fees: loan.fees };
  }

  const assessed = Math.min(rule.lateFee, Math.max(0, rule.lateFeeCap - loan.fees));
  await new Promise((resolve) => setImmediate(resolve)); // let the event loop breathe between loans
  const updated = { ...loan, fees: loan.fees + assessed, version: loan.version + 1 };
  await loanAccounts.put(loanId, updated);

  logger.info({ loanId, assessed }, 'part2.fees.assessed');
  return { loanId, assessed, fees: updated.fees };
}

module.exports = { assessLateFee };
