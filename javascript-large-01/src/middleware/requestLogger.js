'use strict';

const { logger } = require('../utils/logger');
const { redactHeaders } = require('../utils/redact');

/**
 * Structured request/response logging middleware.
 *
 * Every inbound header map is run through redactHeaders() before it is handed
 * to the logger, so authorization tokens, cookies and partner keys are never
 * written to a log sink.
 */
function requestLogger() {
  return function logRequest(req, res, next) {
    const startedAt = process.hrtime.bigint();
    const safeHeaders = redactHeaders(req.headers);

    logger.info(
      {
        method: req.method,
        path: req.originalUrl,
        headers: safeHeaders,
        remote: req.ip,
      },
      'request.received'
    );

    res.on('finish', () => {
      const elapsedMs = Number(process.hrtime.bigint() - startedAt) / 1e6;
      logger.info(
        {
          method: req.method,
          path: req.originalUrl,
          status: res.statusCode,
          elapsedMs: Math.round(elapsedMs),
        },
        'request.completed'
      );
    });

    next();
  };
}

module.exports = { requestLogger };
