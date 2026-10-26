'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const collectionsService = require('../services/collectionsService');
const feeAccrualService = require('../services/feeAccrualService');
const { assertSafeId, ValidationError } = require('../../utils/validate');

const router = express.Router();

/**
 * Refer a delinquent loan to the collections agency.
 */
router.post('/referrals/:loanId', authenticate(), requireRole('collections'), async (req, res, next) => {
  try {
    const loanId = assertSafeId(req.params.loanId);
    const subjectRef = assertSafeId(req.body.subjectRef);
    const result = await collectionsService.referLoan(loanId, subjectRef);
    return res.status(201).json(result);
  } catch (err) {
    return next(err);
  }
});

/**
 * Nightly accrual run, invoked by the scheduler.
 */
router.post('/accruals/run', authenticate(), requireRole('ops'), async (req, res, next) => {
  try {
    const asOf = typeof req.body.asOf === 'string' ? req.body.asOf : new Date().toISOString().slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(asOf)) {
      throw new ValidationError('asOf must be YYYY-MM-DD');
    }
    return res.json(await feeAccrualService.accrueAll(asOf));
  } catch (err) {
    return next(err);
  }
});

router.post('/fees/:loanId/waive', authenticate(), requireRole('collections'), async (req, res, next) => {
  try {
    const amount = Number(req.body.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      throw new ValidationError('amount must be positive');
    }
    const row = await feeAccrualService.waiveLateFees(assertSafeId(req.params.loanId), amount, req.user.id);
    if (!row) {
      return res.status(404).json({ error: 'NotFound', message: 'no accrual record' });
    }
    return res.json({ loanId: row.loanId, lateFees: row.lateFees });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
