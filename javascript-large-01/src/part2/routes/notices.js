'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const { requireLoanOwnership } = require('../guards/loanOwnership');
const { composeNotice } = require('../services/noticeComposer');
const { logger } = require('../../utils/logger');

const router = express.Router();

/**
 * Preview a borrower notice as HTML. Used by servicing agents before a
 * notice is queued for print/email. The agent may attach a free-text note.
 */
router.post(
  '/:loanId/preview',
  authenticate(),
  requireRole('servicing:write'),
  requireLoanOwnership(),
  (req, res, next) => {
    try {
      const body = req.body || {};
      const html = composeNotice(body.kind, {
        recipientName: body.recipientName,
        loanId: req.loan.id,
        amountDue: body.amountDue,
        dueDate: body.dueDate,
        note: body.note,
        reference: `NTC-${req.loan.id}-${Date.now()}`,
      });
      logger.info({ loanId: req.loan.id, kind: body.kind }, 'part2.notice.previewed');
      res.setHeader('content-type', 'text/html; charset=utf-8');
      return res.send(html);
    } catch (err) {
      return next(err);
    }
  }
);

module.exports = router;
