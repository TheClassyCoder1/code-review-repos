'use strict';

const { customerRouter, staffRouter } = require('./disputeRoutes');
const { startEscalationSweep } = require('./escalationSweep');

/**
 * Mount the statement-dispute feature on an Express app. Call before the 404
 * handler is registered:
 *
 *   const { mountDisputes } = require('./part7');
 *   mountDisputes(app);
 */
function mountDisputes(app, { sweep = false } = {}) {
  app.use('/api/v1/disputes', customerRouter());
  app.use('/api/v1/servicing/disputes', staffRouter());
  return sweep ? startEscalationSweep() : null;
}

module.exports = { mountDisputes };
