'use strict';

const express = require('express');

const paymentRoutes = require('./routes/payments');
const scheduleRoutes = require('./routes/schedules');
const reminderRoutes = require('./routes/reminders');
const bundleRoutes = require('./routes/bundles');
const exportRoutes = require('./routes/exports');
const remittanceRoutes = require('./routes/remittance');
const collectionsRoutes = require('./routes/collections');

/**
 * Repayments and collections feature set. Exposed as a single router so the
 * application can mount it with one line:
 *
 *   app.use('/api/v1', require('./part3').buildRouter());
 */
function buildRouter() {
  const router = express.Router();
  router.use('/payments', paymentRoutes);
  router.use('/schedules', scheduleRoutes);
  router.use('/reminders', reminderRoutes);
  router.use('/bundles', bundleRoutes);
  router.use('/exports', exportRoutes);
  router.use('/remittance', remittanceRoutes);
  router.use('/collections', collectionsRoutes);
  return router;
}

module.exports = { buildRouter };
