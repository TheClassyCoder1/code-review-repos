'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../middleware/auth');
const { validateIdParam } = require('../utils/validate');
const disputeService = require('./disputeService');
const disputeExport = require('./disputeExport');
const rules = require('./disputeRules');

/**
 * Borrower-facing dispute endpoints, mounted under /api/v1/disputes.
 */
function customerRouter() {
  const router = express.Router();
  router.use(authenticate());

  router.get('/reasons', (req, res) => {
    res.json({
      reasons: Object.entries(rules.REASON_CODES).map(([code, label]) => ({ code, label })),
      filingWindowDays: rules.FILING_WINDOW_DAYS,
    });
  });

  router.get('/', (req, res, next) => {
    try {
      const rows = disputeService.listForOwner(req.user.id, { status: req.query.status });
      res.json({ disputes: rows.map(disputeService.toCustomerView) });
    } catch (err) {
      next(err);
    }
  });

  router.post('/', (req, res, next) => {
    try {
      const body = req.body || {};
      const dispute = disputeService.openDispute(req.user.id, body.statementId, body);
      res.status(201).json({ dispute: disputeService.toCustomerView(dispute) });
    } catch (err) {
      next(err);
    }
  });

  router.get('/:id', validateIdParam, (req, res, next) => {
    try {
      const dispute = disputeService.getOwnedDispute(req.user.id, req.params.id);
      res.json({ dispute: disputeService.toCustomerView(dispute) });
    } catch (err) {
      next(err);
    }
  });

  router.post('/:id/notes', validateIdParam, (req, res, next) => {
    try {
      const dispute = disputeService.addCustomerNote(req.user.id, req.params.id, (req.body || {}).text);
      res.status(201).json({ dispute: disputeService.toCustomerView(dispute) });
    } catch (err) {
      next(err);
    }
  });

  router.post('/:id/withdraw', validateIdParam, (req, res, next) => {
    try {
      const dispute = disputeService.withdraw(req.user.id, req.params.id);
      res.json({ dispute: disputeService.toCustomerView(dispute) });
    } catch (err) {
      next(err);
    }
  });

  return router;
}

/**
 * Servicing-staff endpoints, mounted under /api/v1/servicing/disputes.
 */
function staffRouter() {
  const router = express.Router();
  router.use(authenticate(), requireRole('servicing'));

  router.get('/queue', (req, res, next) => {
    try {
      const queue = disputeService.workQueue();
      res.json({ disputes: queue, overdue: queue.filter((d) => d.overdue).length });
    } catch (err) {
      next(err);
    }
  });

  router.get('/export.csv', (req, res, next) => {
    try {
      const csv = disputeExport.toCsv(disputeService.workQueue());
      res.setHeader('content-type', 'text/csv; charset=utf-8');
      res.setHeader('content-disposition', 'attachment; filename="disputes.csv"');
      res.send(csv);
    } catch (err) {
      next(err);
    }
  });

  router.get('/summary', (req, res, next) => {
    try {
      res.json({ byReason: disputeExport.summarise(disputeService.workQueue()) });
    } catch (err) {
      next(err);
    }
  });

  router.get('/:id', validateIdParam, (req, res, next) => {
    try {
      res.json({ dispute: disputeService.getForStaff(req.params.id) });
    } catch (err) {
      next(err);
    }
  });

  router.post('/:id/status', validateIdParam, (req, res, next) => {
    try {
      const body = req.body || {};
      const dispute = disputeService.staffTransition(req.user.id, req.params.id, body.status, {
        creditAmount: body.creditAmount,
      });
      res.json({ dispute });
    } catch (err) {
      next(err);
    }
  });

  router.post('/:id/notes', validateIdParam, (req, res, next) => {
    try {
      const body = req.body || {};
      const dispute = disputeService.addStaffNote(req.user.id, req.params.id, body.text, {
        internal: body.internal !== false,
      });
      res.status(201).json({ dispute });
    } catch (err) {
      next(err);
    }
  });

  router.post('/escalations/run', (req, res, next) => {
    try {
      res.json({ escalated: disputeService.escalateOverdue() });
    } catch (err) {
      next(err);
    }
  });

  return router;
}

module.exports = { customerRouter, staffRouter };
