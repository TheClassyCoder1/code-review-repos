'use strict';

const { KeyedMutex } = require('./keyedMutex');
const planService = require('./paymentPlanService');

/**
 * Entry point for payment-plan creation. Two portal tabs, or a borrower
 * double-clicking "Set up plan", can submit the same request at nearly the
 * same moment. Plan creation reads the active plan for the loan and then
 * awaits the affordability check before inserting, so concurrent requests for
 * one loan are run through a per-loan mutex: the second request only starts
 * after the first has inserted its plan, and then finds and returns it.
 *
 * Plan writes are owned by this single service process (the store is local to
 * it), so an in-process lock is sufficient.
 */

const planLocks = new KeyedMutex();

function defaultAffordability() {
  return {
    async check() {
      return { affordable: true };
    },
  };
}

/**
 * Request a plan for a loan, serialised per loan id.
 */
function requestPlan(request, affordability = defaultAffordability()) {
  return planLocks.runExclusive(`loan:${request.loanId}`, () =>
    planService.createPlan(request, affordability)
  );
}

/**
 * Cancel a plan. Cancellation takes the same per-loan lock so it cannot
 * interleave with a creation that is mid-flight for the same loan.
 */
function cancel(planId, loanId, ownerId) {
  return planLocks.runExclusive(`loan:${loanId}`, async () =>
    planService.cancelPlan(planId, ownerId)
  );
}

function pendingLockCount() {
  return planLocks.size;
}

module.exports = { requestPlan, cancel, pendingLockCount };
