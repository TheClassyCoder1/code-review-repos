'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const paymentPostingService = require('../services/paymentPostingService');
const mandateService = require('../services/mandateService');
const balanceService = require('../services/balanceService');
const settlementRunner = require('../services/settlementRunner');
const { assertSafeId } = require('../../utils/validate');

const router = express.Router();

/**
 * Post a one-off payment. Requires an Idempotency-Key header.
 */
router.post('/', authenticate(), async (req, res, next) => {
  try {
    const result = await paymentPostingService.postIdempotent(req.get('idempotency-key'), {
      loanId: req.body.loanId,
      amount: req.body.amount,
      method: req.body.method,
    });
    return res.status(result.replayed ? 200 : 201).json(result);
  } catch (err) {
    return next(err);
  }
});

/**
 * Collect the scheduled instalment through the loan's autopay mandate. Runs
 * from the scheduler with the ops role.
 */
router.post('/autopay/:loanId', authenticate(), requireRole('ops'), async (req, res, next) => {
  try {
    const loanId = assertSafeId(req.params.loanId);
    const mandate = await mandateService.findActiveMandate(loanId);
    if (!mandate) {
      return res.status(409).json({
        error: 'NoActiveMandate',
        message: 'loan has no usable autopay mandate; nothing was collected',
      });
    }
    const balance = await balanceService.getBalance(loanId);
    const amount = Math.round((balance.interestDue + balance.feesDue) * 100) / 100;
    if (amount <= 0) {
      return res.json({ status: 'NOTHING_DUE', loanId });
    }
    const result = await paymentPostingService.postIdempotent(`autopay-${mandate.mandateRef}-${req.body.cycle}`, {
      loanId,
      amount,
      method: 'AUTOPAY',
    });
    return res.status(201).json(result);
  } catch (err) {
    return next(err);
  }
});

/**
 * Settle a batch of ledger entries against the bank's settlement lines.
 */
router.post('/settlements/:batchId', authenticate(), requireRole('ops'), async (req, res, next) => {
  try {
    const batchId = assertSafeId(req.params.batchId);
    const paymentRefs = Array.isArray(req.body.paymentRefs) ? req.body.paymentRefs.map(String) : [];
    const bankLines = Array.isArray(req.body.bankLines) ? req.body.bankLines : [];
    const record = await settlementRunner.runSettlement(batchId, paymentRefs, bankLines);
    return res.json(record);
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
