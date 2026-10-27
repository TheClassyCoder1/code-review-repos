'use strict';

const express = require('express');
const { authenticate } = require('../../middleware/auth');
const { validateIdParam } = require('../../utils/validate');
const { parsePageParams, paginateById } = require('../http/pagination');
const { summariseTransactions, groupByPeriod } = require('../summary/statementSummary');
const { toCsv, LINE_ITEM_COLUMNS } = require('../export/statementCsv');

/**
 * Statement delivery endpoints, mounted under /api/v1/deliveries. These are new
 * endpoints; none of the existing /api/v1 statement response shapes change.
 *
 * Handlers never touch a store directly: ownership goes through the statement
 * service and delivery state through the delivery service, both injected.
 */
function buildDeliveryRouter({ statementService, deliveryService, lineItems }) {
  const router = express.Router();

  /**
   * Queue a delivery for one of the caller's statements.
   */
  router.post('/statements/:id', authenticate(), validateIdParam, (req, res, next) => {
    try {
      const statement = statementService.getOwnedStatement(req.params.id, req.user.id);
      if (!statement) {
        return res.status(404).json({ error: 'NotFound', message: 'no such statement' });
      }

      const body = req.body || {};
      const job = deliveryService.enqueueStatementDelivery(statement, body.channels, body.quietHours);
      return res.status(202).json({
        deliveryId: job.id,
        statementId: job.statementId,
        state: job.state,
        notBefore: new Date(job.notBefore).toISOString(),
        channels: job.preferences,
      });
    } catch (err) {
      if (err.status === 409) {
        return res.status(409).json({ error: 'Conflict', message: err.message });
      }
      if (err.status === 400 || err.name === 'DeliveryWindowError') {
        return res.status(400).json({ error: 'BadRequest', message: err.message });
      }
      return next(err);
    }
  });

  /**
   * Page through the caller's delivery history.
   */
  router.get('/', authenticate(), (req, res, next) => {
    try {
      const page = parsePageParams(req.query);
      const history = deliveryService.historyForOwner(req.user.id);
      const { items, nextCursor } = paginateById(history, page);
      return res.json({ deliveries: items, nextCursor });
    } catch (err) {
      return next(err);
    }
  });

  /**
   * Per-period delivered/pending rollup of the caller's statements.
   */
  router.get('/summary', authenticate(), (req, res, next) => {
    try {
      const statements = statementService.listForOwner(req.user.id);
      const delivered = new Set(
        deliveryService
          .historyForOwner(req.user.id)
          .filter((d) => d.state === 'delivered')
          .map((d) => d.statementId)
      );
      const rows = statements.map((s) => ({ period: s.period, deliveredAt: delivered.has(s.id) }));
      return res.json(groupByPeriod(rows));
    } catch (err) {
      return next(err);
    }
  });

  /**
   * Download the line items of an owned statement as CSV, with a summary
   * header returned alongside as response headers.
   */
  router.get('/statements/:id/line-items.csv', authenticate(), validateIdParam, async (req, res, next) => {
    try {
      const statement = statementService.getOwnedStatement(req.params.id, req.user.id);
      if (!statement) {
        return res.status(404).json({ error: 'NotFound', message: 'no such statement' });
      }
      const items = await lineItems.forStatement(statement.id);
      const summary = summariseTransactions(statement.openingBalanceCents || 0, items);
      res.setHeader('content-type', 'text/csv; charset=utf-8');
      res.setHeader('content-disposition', `attachment; filename="${statement.id}-line-items.csv"`);
      res.setHeader('x-closing-balance-cents', String(summary.closingBalanceCents));
      return res.send(toCsv(items, LINE_ITEM_COLUMNS));
    } catch (err) {
      return next(err);
    }
  });

  return router;
}

module.exports = { buildDeliveryRouter };
