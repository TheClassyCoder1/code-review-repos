'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const { requireLoanOwnership } = require('../guards/loanOwnership');
const autopay = require('../services/autopayScheduler');
const fees = require('../services/feeAccrual');
const ptp = require('../services/promiseToPayTracker');
const agency = require('../services/collectionsAgencyClient');
const bureau = require('../services/bureauClient');
const rules = require('../services/servicingRules');
const tokens = require('../services/agentTokenIssuer');

const router = express.Router();

/**
 * Borrower: enroll a loan in autopay.
 */
router.post('/:loanId/autopay', authenticate(), requireLoanOwnership(), async (req, res, next) => {
  try {
    const body = req.body || {};
    const result = await autopay.enroll(req.loan, {
      dayOfMonth: Number(body.dayOfMonth),
      fundingAccountToken: body.fundingAccountToken,
    });
    return res.status(201).json(result);
  } catch (err) {
    return next(err);
  }
});

/**
 * Agent: assess a late fee.
 */
router.post('/:loanId/fees/assess', authenticate(), requireRole('servicing:write'), async (req, res, next) => {
  try {
    return res.json(await fees.assessLateFee(req.params.loanId));
  } catch (err) {
    return next(err);
  }
});

/**
 * Agent: record promises to pay captured on a collections call.
 */
router.post('/:loanId/promises', authenticate(), requireRole('servicing:write'), async (req, res, next) => {
  try {
    const arrangements = Array.isArray(req.body && req.body.arrangements) ? req.body.arrangements : [];
    if (arrangements.length === 0 || arrangements.length > 6) {
      return res.status(400).json({ error: 'BadRequest', message: '1 to 6 arrangements required' });
    }
    const recorded = await ptp.recordPromises(req.params.loanId, req.user.id, arrangements);
    return res.status(201).json({ recorded, summary: ptp.summary(req.params.loanId) });
  } catch (err) {
    return next(err);
  }
});

/**
 * Agent: place a seriously delinquent loan with the collections agency,
 * refreshing the bureau view first.
 */
router.post('/:loanId/placement', authenticate(), requireRole('servicing:write'), requireLoanOwnership(), async (req, res, next) => {
  try {
    const rule = rules.forProduct(req.loan.planId.split('-')[0]);
    const threshold = rule ? rule.placementDays : 90;
    if (req.loan.daysPastDue < threshold) {
      return res.status(409).json({ error: 'Conflict', message: 'loan below placement threshold' });
    }
    const refresh = await bureau.softRefresh(req.loan.borrowerId, 'pre_placement');
    const placementId = await agency.placeAccount(req.loan);
    return res.status(201).json({ placementId, bureauReportId: refresh.reportId });
  } catch (err) {
    return next(err);
  }
});

/**
 * Ops: dry-run a servicing rules document.
 */
router.post('/rules/dry-run', authenticate(), requireRole('servicing:admin'), express.text({ type: '*/*', limit: '256kb' }), (req, res, next) => {
  try {
    return res.json(rules.dryRun(req.body));
  } catch (err) {
    return next(Object.assign(err, { status: err.status || 422 }));
  }
});

/**
 * Ops: issue a field-agent device token.
 */
router.post('/agent-tokens', authenticate(), requireRole('servicing:admin'), (req, res) => {
  const agentId = String((req.body && req.body.agentId) || '');
  if (!/^agent-[0-9]{1,8}$/.test(agentId)) {
    return res.status(400).json({ error: 'BadRequest', message: 'invalid agent id' });
  }
  return res.status(201).json(tokens.issue(agentId));
});

module.exports = router;
