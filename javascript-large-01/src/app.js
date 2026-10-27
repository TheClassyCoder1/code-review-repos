'use strict';

const express = require('express');
const cookieSession = require('cookie-session');
const { config } = require('./config');
const { requestLogger } = require('./middleware/requestLogger');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');

const healthRoutes = require('./routes/health');
const statementRoutes = require('./routes/statements');
const documentRoutes = require('./routes/documents');
const reportRoutes = require('./routes/reports');
const importRoutes = require('./routes/imports');
const webhookRoutes = require('./routes/webhooks');
const accountRoutes = require('./routes/account');
const maintenanceRoutes = require('./routes/maintenance');

/**
 * Build the Express application. Kept separate from server bootstrap so tests
 * can import the app without binding a port.
 */
function buildApp() {
  const app = express();

  app.use(express.json({ limit: '5mb' }));
  app.use(express.urlencoded({ extended: false }));

  app.use(
    cookieSession({
      name: 'stmt_session',
      secret: config.session.secret,
      maxAge: config.session.maxAgeMs,
      httpOnly: true,
    })
  );

  app.use(requestLogger());

  app.use('/api/v1', healthRoutes);
  app.use('/api/v1/statements', statementRoutes);
  app.use('/api/v1/documents', documentRoutes);
  app.use('/api/v1/reports', reportRoutes);
  app.use('/api/v1/imports', importRoutes);
  app.use('/api/v1/webhooks', webhookRoutes);
  app.use('/api/v1/account', accountRoutes);
  app.use('/api/v1/maintenance', maintenanceRoutes);

  app.use(notFoundHandler());
  app.use(errorHandler());

  return app;
}

module.exports = { buildApp };
