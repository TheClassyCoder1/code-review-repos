'use strict';

const { toCents, fromCents, roundHalfEven } = require('./money');
const { daysBetween, addMonths } = require('./businessDays');

/**
 * Payoff quotes and portfolio summaries for active payment plans.
 */

const QUOTE_VALID_DAYS = 10;

/**
 * Quote the amount needed to pay a plan off on `payoffDay`. Unpaid principal
 * from every remaining installment is due in full; interest accrues daily on
 * that principal from the last paid due date (or plan start) to the payoff
 * day. Interest already scheduled but not yet earned is not charged.
 */
function payoffQuote(plan, payoffDay, rateBps) {
  if (!plan || !Array.isArray(plan.schedule) || plan.schedule.length === 0) {
    throw new TypeError('plan with a schedule is required');
  }

  const unpaid = plan.schedule.filter((inst) => !inst.paid);
  if (unpaid.length === 0) {
    return { planId: plan.id, payoffDay, principal: 0, interest: 0, total: 0, validUntil: payoffDay };
  }

  const principalCents = unpaid.reduce((sum, inst) => sum + toCents(inst.principal), 0);
  const paid = plan.schedule.filter((inst) => inst.paid);
  const accrualStart = paid.length > 0 ? paid[paid.length - 1].dueDate : plan.startDay;
  const accrualDays = Math.max(0, daysBetween(accrualStart, payoffDay));
  const interestCents = roundHalfEven((principalCents * (rateBps / 10000) * accrualDays) / 365);

  return {
    planId: plan.id,
    payoffDay,
    principal: fromCents(principalCents),
    interest: fromCents(interestCents),
    total: fromCents(principalCents + interestCents),
    validUntil: addDays(payoffDay, QUOTE_VALID_DAYS),
  };
}

function addDays(day, days) {
  const date = new Date(`${day}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

/**
 * Summarise a set of plans for the operations dashboard: counts per status,
 * the outstanding scheduled amount on active plans, and how many active plans
 * have an installment due within the next `horizonMonths` months.
 */
function summarizePlans(plans, today, horizonMonths = 1) {
  const horizon = addMonths(today, horizonMonths);
  const byStatus = {};
  let outstandingCents = 0;
  let dueSoon = 0;

  for (const plan of plans) {
    byStatus[plan.status] = (byStatus[plan.status] || 0) + 1;
    if (plan.status !== 'active') {
      continue;
    }
    const unpaid = plan.schedule.filter((inst) => !inst.paid);
    outstandingCents += unpaid.reduce((sum, inst) => sum + toCents(inst.amountDue), 0);
    const hasDueSoon = unpaid.some((inst) => inst.dueDate >= today && inst.dueDate <= horizon);
    if (hasDueSoon) {
      dueSoon += 1;
    }
  }

  return {
    total: plans.length,
    byStatus,
    activeOutstanding: fromCents(outstandingCents),
    activeDueWithinHorizon: dueSoon,
    horizon,
  };
}

/**
 * Find the next unpaid installment on or after `today`, or the oldest overdue
 * one if any are overdue. Overdue installments take priority because the
 * portal shows the borrower what to pay first.
 */
function nextInstallmentToPay(plan, today) {
  if (!plan || !Array.isArray(plan.schedule)) {
    throw new TypeError('plan with a schedule is required');
  }

  let oldestOverdue = null;
  let nextUpcoming = null;

  for (const inst of plan.schedule) {
    if (inst.paid) {
      continue;
    }
    if (inst.dueDate < today) {
      if (!oldestOverdue || inst.dueDate < oldestOverdue.dueDate) {
        oldestOverdue = inst;
      }
    } else if (!nextUpcoming || inst.dueDate < nextUpcoming.dueDate) {
      nextUpcoming = inst;
    }
  }

  const chosen = oldestOverdue || nextUpcoming;
  if (!chosen) {
    return null;
  }
  return { sequence: chosen.sequence, dueDate: chosen.dueDate, amountDue: chosen.amountDue, overdue: chosen === oldestOverdue };
}

module.exports = { payoffQuote, summarizePlans, nextInstallmentToPay, QUOTE_VALID_DAYS };
