'use strict';

const store = require('./paymentPlanStore');
const { collectionsConfig } = require('./config');
const { toCents, fromCents, roundHalfEven, splitEvenly } = require('./money');
const { addMonths, nextBusinessDay } = require('./businessDays');
const { logger } = require('../utils/logger');

/**
 * Payment plans let a delinquent borrower repay an arrears balance in equal
 * monthly installments. This module holds the plan rules; request handling and
 * concurrency control live in paymentPlanCoordinator.
 */

class PlanError extends Error {
  constructor(message, status = 422) {
    super(message);
    this.name = 'PlanError';
    this.status = status;
  }
}

/**
 * Build the installment schedule for a plan. Interest is simple interest on
 * the declining balance, accrued monthly and rounded half-to-even; the
 * principal is split so the installments sum exactly to the balance. Due dates
 * roll forward to the next business day.
 */
function buildSchedule(balance, installments, firstDueDay, rateBps = collectionsConfig.plans.defaultRateBps) {
  const { minInstallments, maxInstallments } = collectionsConfig.plans;
  if (!Number.isInteger(installments) || installments < minInstallments || installments > maxInstallments) {
    throw new PlanError(`installments must be between ${minInstallments} and ${maxInstallments}`);
  }

  const principalParts = splitEvenly(toCents(balance), installments);
  const monthlyRate = rateBps / 10000 / 12;
  let remaining = toCents(balance);

  return principalParts.map((principalCents, index) => {
    const interestCents = roundHalfEven(remaining * monthlyRate);
    remaining -= principalCents;
    const scheduledDay = addMonths(firstDueDay, index);
    return {
      sequence: index + 1,
      dueDate: nextBusinessDay(scheduledDay),
      principal: fromCents(principalCents),
      interest: fromCents(interestCents),
      amountDue: fromCents(principalCents + interestCents),
    };
  });
}

/**
 * Create a payment plan for a loan. A loan may only have one active plan, so
 * an existing active plan is returned instead of creating a second one. The
 * affordability check is asynchronous because it consults the borrower's
 * income-verification record.
 *
 * Callers must go through paymentPlanCoordinator.requestPlan, which serialises
 * plan creation per loan.
 */
async function createPlan(request, affordability) {
  const existing = store.findActiveByLoan(request.loanId);
  if (existing) {
    return { plan: existing, created: false };
  }

  const schedule = buildSchedule(request.balance, request.installments, request.firstDueDay);
  const verdict = await affordability.check(request.ownerId, schedule[0].amountDue);
  if (!verdict.affordable) {
    throw new PlanError('requested plan exceeds verified affordability');
  }

  const plan = store.insert({
    loanId: request.loanId,
    ownerId: request.ownerId,
    status: 'active',
    startDay: new Date().toISOString().slice(0, 10),
    balance: request.balance,
    schedule,
  });
  logger.info({ planId: plan.id, loanId: plan.loanId, installments: schedule.length }, 'plan.created');
  return { plan, created: true };
}

/**
 * Cancel a plan owned by the given user. Only active plans can be cancelled.
 */
function cancelPlan(planId, ownerId) {
  const plan = store.findById(planId);
  if (!plan || plan.ownerId !== ownerId) {
    throw new PlanError('no such plan', 404);
  }
  if (plan.status !== 'active') {
    throw new PlanError(`plan is ${plan.status}, not active`, 409);
  }
  logger.info({ planId, loanId: plan.loanId }, 'plan.cancelled');
  return store.updateStatus(planId, 'cancelled');
}

/**
 * Return a plan if it exists and belongs to `ownerId`, otherwise null.
 */
function getOwnedPlan(planId, ownerId) {
  const plan = store.findById(planId);
  if (!plan || plan.ownerId !== ownerId) {
    return null;
  }
  return plan;
}

/**
 * Operator listing of plans in a status, paged by plan id.
 */
function listPlansByStatus(status, page) {
  if (!['active', 'cancelled', 'completed', 'broken'].includes(status)) {
    throw new PlanError(`unknown plan status: ${status}`, 400);
  }
  return store.listByStatus(status, page);
}

module.exports = { buildSchedule, createPlan, cancelPlan, getOwnedPlan, listPlansByStatus, PlanError };
