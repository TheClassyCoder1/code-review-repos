'use strict';

const collectionsAgencyClient = require('../clients/collectionsAgencyClient');
const bureauClient = require('../clients/bureauClient');
const balanceService = require('./balanceService');
const mandateService = require('./mandateService');
const { runWithRetry } = require('./retryRunner');
const { ledger } = require('../stores/repaymentStores');
const { part3Config } = require('../config/part3Config');
const { logger } = require('../../utils/logger');

/**
 * Referral of seriously delinquent loans to the external collections agency.
 */

class NotEligibleError extends Error {
  constructor(message) {
    super(message);
    this.name = 'NotEligibleError';
    this.status = 409;
  }
}

async function repaymentHistory(loanId) {
  const refs = await ledger.keys();
  const rows = [];
  for (const ref of refs) {
    const entry = await ledger.get(ref);
    if (entry && entry.loanId === loanId) {
      rows.push({ paymentRef: entry.paymentRef, amount: entry.amount, receivedAt: entry.receivedAt });
    }
  }
  return rows;
}

/**
 * Refer a loan to collections when it is past the configured days-past-due.
 */
async function referLoan(loanId, subjectRef) {
  const balance = await balanceService.getBalance(loanId);
  if (balance.daysPastDue < part3Config.collections.minDaysPastDue) {
    throw new NotEligibleError(`loan is ${balance.daysPastDue} days past due`);
  }

  const tradelines = await runWithRetry('bureau:summary', () => bureauClient.fetchTradelineSummary(subjectRef));
  const history = await repaymentHistory(loanId);

  const placementRef = await collectionsAgencyClient.placeAccount({
    accountRef: loanId,
    subjectRef,
    outstanding: balance.principal + balance.interestDue + balance.feesDue,
    daysPastDue: balance.daysPastDue,
    openTradelines: tradelines.openTradelines,
    history,
  });

  await mandateService.suspendMandate(loanId, 'COLLECTIONS_PLACEMENT');
  const placedOn = new Date().toISOString().slice(0, 10);
  await runWithRetry('bureau:placement', () => bureauClient.reportPlacement(subjectRef, loanId, placedOn));

  logger.info({ loanId, placementRef }, 'part3.collections.referred');
  return { loanId, placementRef, placedOn };
}

module.exports = { referLoan, NotEligibleError };
