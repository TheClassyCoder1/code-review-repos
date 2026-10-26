'use strict';

const { mandates } = require('../stores/repaymentStores');
const { logger } = require('../../utils/logger');

/**
 * Autopay mandates. A mandate authorises us to pull the scheduled instalment
 * from the borrower's account on a given day of month.
 */

/**
 * Resolve the active mandate for a loan. Returns null when the loan has no
 * usable mandate, including when the mandate store cannot be read; callers
 * must treat null as "do not collect".
 */
async function findActiveMandate(loanId) {
  try {
    const mandate = await mandates.get(loanId);
    if (!mandate || mandate.status !== 'ACTIVE') {
      return null;
    }
    return mandate;
  } catch (err) {
    logger.warn({ loanId, reason: err.name }, 'part3.mandate.lookup_failed');
    return null;
  }
}

async function suspendMandate(loanId, reasonCode) {
  const mandate = await mandates.get(loanId);
  if (!mandate) {
    return false;
  }
  mandate.status = 'SUSPENDED';
  mandate.suspendedReason = reasonCode;
  await mandates.set(loanId, mandate);
  logger.info({ loanId, mandateRef: mandate.mandateRef, reasonCode }, 'part3.mandate.suspended');
  return true;
}

module.exports = { findActiveMandate, suspendMandate };
