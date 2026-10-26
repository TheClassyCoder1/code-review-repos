'use strict';

const express = require('express');
const profileRoutes = require('./routes/profile');
const kycRoutes = require('./routes/kyc');
const deliveryContactRoutes = require('./routes/deliveryContact');
const statementUsageTracker = require('./services/statementUsageTracker');
const { logger } = require('../utils/logger');

const FLUSH_INTERVAL_MS = Number(process.env.USAGE_FLUSH_INTERVAL_MS || 30000);

/**
 * Customer profile, identity verification and delivery-contact routes.
 * Mounted by the app under `/api/v1/customer`:
 *
 *   GET  /api/v1/customer/profile
 *   PUT  /api/v1/customer/profile
 *   GET  /api/v1/customer/kyc/status
 *   POST /api/v1/customer/kyc/verify
 *   POST /api/v1/customer/delivery/email
 *   POST /api/v1/customer/delivery/channel
 */
function customerRouter() {
  const router = express.Router();
  router.use('/profile', profileRoutes);
  router.use('/kyc', kycRoutes);
  router.use('/delivery', deliveryContactRoutes);
  return router;
}

/**
 * Start the periodic usage-event flush. Returns a stop function so tests and
 * graceful shutdown can clear the timer.
 */
function startUsageFlush(intervalMs = FLUSH_INTERVAL_MS) {
  const timer = setInterval(() => {
    statementUsageTracker
      .flush()
      .then((sent) => {
        if (sent > 0) {
          logger.debug({ sent }, 'usage.flush.completed');
        }
      })
      .catch((err) => logger.warn({ err: err.message }, 'usage.flush.failed'));
  }, intervalMs);
  timer.unref();
  return () => clearInterval(timer);
}

module.exports = { customerRouter, startUsageFlush };
