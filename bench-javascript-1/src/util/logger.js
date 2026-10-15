'use strict';

const pino = require('pino');

const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  redact: {
    paths: ['headers.authorization', 'headers["x-api-key"]', 'password', 'token', '*.password'],
    censor: '[REDACTED]',
  },
});

module.exports = logger;
