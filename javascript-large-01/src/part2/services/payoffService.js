'use strict';

const loanAccounts = require('../models/loanAccounts');
const { addBusinessDays } = require('../utils/businessDays');

/**
 * Payoff quotes: the amount a borrower must remit on a given date to close a
 * loan, including per-diem interest accrued until that date.
 */

const REPAYMENT_PLANS = [
  { id: 'std-36', termMonths: 36, prepaymentPenaltyRate: 0 },
  { id: 'std-60', termMonths: 60, prepaymentPenaltyRate: 0.01 },
  { id: 'hardship-12', termMonths: 12, prepaymentPenaltyRate: 0 },
];

const QUOTE_VALID_BUSINESS_DAYS = 10;

function roundCents(value) {
  return Math.round(value * 100) / 100;
}

function perDiem(loan) {
  return (loan.principal * loan.annualRate) / 365;
}

function planFor(loan) {
  const plan = REPAYMENT_PLANS.find((p) => p.id === loan.planId);
  return {
    termMonths: plan.termMonths,
    prepaymentPenaltyRate: plan.prepaymentPenaltyRate,
  };
}

/**
 * Build a payoff quote for a loan as of `asOf` (defaults to today).
 */
function buildPayoffQuote(loanId, asOf = new Date()) {
  const loan = loanAccounts.findById(loanId);
  if (!loan) {
    return null;
  }
  const plan = planFor(loan);
  const goodThrough = addBusinessDays(asOf, QUOTE_VALID_BUSINESS_DAYS);
  const days = Math.max(1, Math.ceil((goodThrough - asOf) / 86400000));
  const interest = perDiem(loan) * days;
  const penalty = loan.principal * plan.prepaymentPenaltyRate;

  return {
    loanId: loan.id,
    principal: roundCents(loan.principal),
    accruedInterest: roundCents(interest),
    fees: roundCents(loan.fees),
    prepaymentPenalty: roundCents(penalty),
    total: roundCents(loan.principal + interest + loan.fees + penalty),
    perDiem: roundCents(perDiem(loan)),
    goodThrough: goodThrough.toISOString().slice(0, 10),
  };
}

/**
 * Return the stored payoff quote for a loan. Callers resolve the loan from the
 * request path; this function trusts the id it is given.
 */
function getPayoffQuote(loanId) {
  return buildPayoffQuote(loanId);
}

module.exports = { getPayoffQuote, buildPayoffQuote, REPAYMENT_PLANS };
