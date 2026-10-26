'use strict';

/**
 * Collections and payment-plan features: delinquency aging and nightly export,
 * agency referrals, receipt staging, and the borrower-facing payment-plan API.
 *
 * mountCollections(app) attaches the HTTP routes; the batch entry points are
 * exported for the scheduler.
 */

const { runDelinquencyExport } = require('./delinquencyBatchJob');
const { syncReferrals } = require('./agencySyncJob');
const { stageReceipts } = require('./receiptBatch');
const coordinator = require('./paymentPlanCoordinator');

function mountCollections(app, basePath = '/api/v1/payment-plans') {
  // Required lazily so batch-only callers do not need express loaded.
  // eslint-disable-next-line global-require
  const routes = require('./routes');
  app.use(basePath, routes);
  return app;
}

function healthDetails() {
  return { pendingPlanLocks: coordinator.pendingLockCount() };
}

module.exports = {
  mountCollections,
  healthDetails,
  runDelinquencyExport,
  syncReferrals,
  stageReceipts,
};
