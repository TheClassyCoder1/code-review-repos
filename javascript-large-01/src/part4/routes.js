'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../middleware/auth');
const { validateIdParam } = require('../utils/validate');
const coordinator = require('./paymentPlanCoordinator');
const planService = require('./paymentPlanService');
const { validatePlanRequest, parsePageQuery } = require('./planValidators');
const { payoffQuote, nextInstallmentToPay, summarizePlans } = require('./payoffQuote');
const { collectionsConfig } = require('./config');
const { toIsoDay, parseIsoDay } = require('./businessDays');

/**
 * Payment-plan endpoints, mounted under /api/v1/payment-plans by
 * part4/index.mountCollections. Handlers validate input and delegate to the
 * coordinator and service modules and never touch the plan store directly.
 */

const router = express.Router();

function asyncHandler(fn) {
  return (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);
}

function isCalendarDay(value) {
  try {
    parseIsoDay(value);
    return true;
  } catch (err) {
    return false;
  }
}

router.post(
  '/',
  authenticate(),
  asyncHandler(async (req, res) => {
    const request = { ...validatePlanRequest(req.body), ownerId: req.user.id };
    const { plan, created } = await coordinator.requestPlan(request);
    if (plan.ownerId !== req.user.id) {
      return res.status(409).json({ error: 'Conflict', message: 'loan already has an active plan' });
    }
    return res.status(created ? 201 : 200).json({ plan });
  })
);

router.get(
  '/:id',
  authenticate(),
  validateIdParam,
  asyncHandler(async (req, res) => {
    const plan = planService.getOwnedPlan(req.params.id, req.user.id);
    if (!plan) {
      return res.status(404).json({ error: 'NotFound', message: 'no such plan' });
    }
    const today = toIsoDay(new Date());
    return res.json({ plan, next: nextInstallmentToPay(plan, today) });
  })
);

/**
 * Quote a payoff amount for one of the caller's plans. The payoff day
 * defaults to today; a caller may ask for a day up to 30 days ahead to plan a
 * transfer. Quotes are informational and are recomputed when the payment
 * actually posts.
 */
router.get(
  '/:id/payoff',
  authenticate(),
  validateIdParam,
  asyncHandler(async (req, res) => {
    const plan = planService.getOwnedPlan(req.params.id, req.user.id);
    if (!plan) {
      return res.status(404).json({ error: 'NotFound', message: 'no such plan' });
    }
    if (plan.status !== 'active') {
      return res.status(409).json({ error: 'Conflict', message: `plan is ${plan.status}` });
    }

    const today = toIsoDay(new Date());
    const payoffDay = typeof req.query.on === 'string' && req.query.on !== '' ? req.query.on : today;
    if (!isCalendarDay(payoffDay) || payoffDay < today) {
      return res.status(400).json({ error: 'ValidationError', message: 'on must be today or a later YYYY-MM-DD day' });
    }
    const latest = new Date(`${today}T00:00:00Z`);
    latest.setUTCDate(latest.getUTCDate() + 30);
    if (payoffDay > toIsoDay(latest)) {
      return res.status(400).json({ error: 'ValidationError', message: 'on must be within 30 days' });
    }

    const quote = payoffQuote(plan, payoffDay, collectionsConfig.plans.defaultRateBps);
    return res.json({ quote });
  })
);

router.delete(
  '/:id',
  authenticate(),
  validateIdParam,
  asyncHandler(async (req, res) => {
    const plan = planService.getOwnedPlan(req.params.id, req.user.id);
    if (!plan) {
      return res.status(404).json({ error: 'NotFound', message: 'no such plan' });
    }
    const cancelled = await coordinator.cancel(plan.id, plan.loanId, req.user.id);
    return res.json({ plan: cancelled });
  })
);

router.get(
  '/',
  authenticate(),
  requireRole('operator'),
  asyncHandler(async (req, res) => {
    const { limit, after } = parsePageQuery(req.query);
    const status = typeof req.query.status === 'string' ? req.query.status : 'active';
    const page = planService.listPlansByStatus(status, { limit, after });
    const summary = summarizePlans(page.plans, toIsoDay(new Date()));
    return res.json({ ...page, summary });
  })
);

module.exports = router;
