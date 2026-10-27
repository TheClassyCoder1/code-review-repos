'use strict';

const { buildApp } = require('./app');
const { config, assertRequiredEnv } = require('./config');
const { logger } = require('./utils/logger');

/**
 * Service entrypoint. Validates required secrets, builds the app and starts
 * listening. Required secrets are enforced before the port is bound so a
 * misconfigured deployment fails fast instead of serving with placeholders.
 */
function main() {
  assertRequiredEnv();

  const app = buildApp();
  const server = app.listen(config.port, () => {
    logger.info({ port: config.port, env: config.env }, 'service.started');
  });

  const shutdown = (signal) => {
    logger.info({ signal }, 'service.stopping');
    server.close(() => process.exit(0));
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));

  return server;
}

if (require.main === module) {
  main();
}

module.exports = { main };
