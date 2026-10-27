'use strict';

const express = require('express');
const { sanitizeReturnTo } = require('./guards/returnTo');

/**
 * Servicing & collections router, mounted by the app under /api/v1/servicing.
 */
function buildServicingRouter() {
  const router = express.Router();

  router.use('/payoff', require('./routes/payoff'));
  router.use('/notices', require('./routes/notices'));
  router.use('/statements', require('./routes/encryptedStatements'));
  router.use('/callbacks', require('./routes/callbacks'));
  router.use('/feeds', require('./routes/partnerFeed'));
  router.use('/receipts', require('./routes/receipts'));
  router.use('/delinquency', require('./routes/delinquency'));
  router.use('/links', sanitizeReturnTo('/servicing'), require('./routes/portalLinks'));
  router.use('/sso', require('./routes/sso'));
  router.use('/go', require('./routes/deepLinks'));
  router.use('/return', require('./routes/partnerReturn'));
  router.use('/loans', require('./routes/servicing'));

  return router;
}

module.exports = { buildServicingRouter };
