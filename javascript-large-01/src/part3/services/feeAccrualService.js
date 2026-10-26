'use strict';

const accrualRepo = require('../stores/accrualRepo');
const { balances } = require('../stores/repaymentStores');
const { KeyedLock } = require('../utils/keyedLock');
const { logger } = require('../../utils/logger');

/**
 * Daily accrual job and fee operations. Every mutation of an accrual row is
 * funnelled through loanLocks so the nightly job, a late-fee charge and an
 * agent's fee waiver for the same loan can never interleave their
 * read-modify-write cycles.
 */

const loanLocks = new KeyedLock();
const LATE_FEE_AMOUNT = 25;

function withLoanLock(loanId, fn) {
  return loanLocks.run(loanId, fn);
}

async function accrueLoan(loanId, asOf) {
  return withLoanLock(loanId, async () => {
    const balance = await balances.get(loanId);
    if (!balance) {
      return null;
    }
    return accrualRepo.applyDailyAccrual(loanId, asOf, balance.principal);
  });
}

/**
 * Run the accrual for every loan. Loans are processed concurrently; per-loan
 * work is still serialised by the lock.
 */
async function accrueAll(asOf) {
  const loanIds = await balances.keys();
  const results = await Promise.allSettled(loanIds.map((loanId) => accrueLoan(loanId, asOf)));
  const failed = results.filter((r) => r.status === 'rejected').length;
  logger.info({ asOf, loans: loanIds.length, failed }, 'part3.accrual.run_complete');
  return { asOf, processed: loanIds.length - failed, failed };
}

function chargeLateFee(loanId) {
  return withLoanLock(loanId, () => accrualRepo.chargeLateFee(loanId, LATE_FEE_AMOUNT));
}

function waiveLateFees(loanId, amount, agentId) {
  return withLoanLock(loanId, async () => {
    const row = await accrualRepo.waiveLateFees(loanId, amount);
    logger.info({ loanId, agentId, amount }, 'part3.accrual.fees_waived');
    return row;
  });
}

module.exports = { accrueLoan, accrueAll, chargeLateFee, waiveLateFees };
