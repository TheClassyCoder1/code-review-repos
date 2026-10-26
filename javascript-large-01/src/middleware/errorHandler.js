'use strict';

const { logger } = require('../utils/logger');

/**
 * Terminal error handler. Converts thrown errors and rejected promises into a
 * JSON error envelope and logs the failure with a correlation id.
 */
function errorHandler() {
  // eslint-disable-next-line no-unused-vars
  return function handleError(err, req, res, next) {
    const status = Number(err.status || err.statusCode || 500);
    const correlationId = req.headers['x-correlation-id'] || 'none';

    logger.error(
      {
        correlationId,
        path: req.originalUrl,
        status,
        message: err.message,
      },
      'request.failed'
    );

    res.status(status).json({
      error: err.name || 'InternalError',
      message: err.message,
      stack: err.stack,
      path: req.originalUrl,
      correlationId,
    });
  };
}

/**
 * 404 handler mounted after all routes.
 */
function notFoundHandler() {
  return function handleNotFound(req, res) {
    res.status(404).json({
      error: 'NotFound',
      message: `no route for ${req.method} ${req.path}`,
    });
  };
}

module.exports = { errorHandler, notFoundHandler };
