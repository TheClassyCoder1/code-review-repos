'use strict';

const { collectionsConfig } = require('./config');

/**
 * Hardship eligibility for payment plans. A borrower qualifies for a plan when
 * the loan is delinquent but not yet charged off, there is no active plan, and
 * the borrower has not broken more than one plan in the past year. The result
 * explains every failed rule so the portal can tell the borrower why.
 */

const CHARGE_OFF_DAYS = 180;
const BROKEN_PLAN_LOOKBACK_DAYS = 365;
const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Assess eligibility. `history` is the list of the borrower's past plans for
 * this loan, each with a status and an updatedAt timestamp.
 */
function assessHardshipEligibility(aged, history, now = Date.now()) {
  const reasons = [];

  if (!aged || aged.daysPastDue <= 0) {
    reasons.push('loan is not delinquent');
  } else if (aged.daysPastDue >= CHARGE_OFF_DAYS) {
    reasons.push('loan has reached charge-off and is handled by recoveries');
  }

  const plans = Array.isArray(history) ? history : [];
  if (plans.some((plan) => plan.status === 'active')) {
    reasons.push('loan already has an active payment plan');
  }

  const cutoff = now - BROKEN_PLAN_LOOKBACK_DAYS * DAY_MS;
  const recentBroken = plans.filter((plan) => {
    const updated = Date.parse(plan.updatedAt);
    return plan.status === 'broken' && Number.isFinite(updated) && updated >= cutoff;
  }).length;
  if (recentBroken > 1) {
    reasons.push('more than one plan was broken in the past year');
  }

  return {
    eligible: reasons.length === 0,
    reasons,
    maxInstallments: recentBroken === 1 ? Math.min(6, collectionsConfig.plans.maxInstallments) : collectionsConfig.plans.maxInstallments,
  };
}

module.exports = { assessHardshipEligibility, CHARGE_OFF_DAYS };
