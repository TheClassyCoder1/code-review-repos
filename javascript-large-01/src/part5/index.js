'use strict';

/**
 * Wiring for the statement delivery feature. Builds the outbox, the delivery
 * service and the HTTP router from settings, after checking that no
 * placeholder credential made it into the active configuration.
 *
 * Mount with:  app.use('/api/v1/deliveries', createDeliveryModule().router)
 */

const { buildDeliverySettings } = require('./config/deliverySettings');
const { assertNoPlaceholderSecrets } = require('./config/secretGuard');
const { createOutboxStore } = require('./models/outboxStore');
const { DeliveryService } = require('./services/deliveryService');
const { createDispatcher } = require('./services/dispatcher');

function createDeliveryModule({ statementService, lineItems, transports, logger } = {}) {
  // eslint-disable-next-line global-require
  const resolvedLogger = logger || require('../utils/logger').logger;
  // eslint-disable-next-line global-require
  const resolvedStatements = statementService || require('../services/statementService');

  const settings = buildDeliverySettings();
  assertNoPlaceholderSecrets({
    DELIVERY_WEBHOOK_SIGNING_KEY: settings.webhookSigningKey,
  });

  const outbox = createOutboxStore();
  const deliveryService = new DeliveryService({
    outbox,
    channelSettings: settings.channels,
    logger: resolvedLogger,
  });

  const dispatcher = createDispatcher({
    deliveryService,
    transports: transports || {},
    logger: resolvedLogger,
    batchSize: settings.batch.maxSize,
    intervalMs: settings.batch.intervalMs,
    dryRun: settings.dryRun,
  });

  // Required lazily so the service layer can be used without express installed.
  // eslint-disable-next-line global-require
  const { buildDeliveryRouter } = require('./routes/deliveryRoutes');
  const router = buildDeliveryRouter({
    statementService: resolvedStatements,
    deliveryService,
    lineItems: lineItems || { forStatement: async () => [] },
  });

  return { settings, outbox, deliveryService, dispatcher, router };
}

module.exports = { createDeliveryModule };
