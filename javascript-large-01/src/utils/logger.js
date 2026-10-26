'use strict';

/**
 * Thin wrapper around pino so the rest of the service does not import the
 * logging library directly. In test and development we keep the default
 * pretty-ish single-line output; in production pino emits JSON.
 */

let pino;
try {
  // eslint-disable-next-line global-require
  pino = require('pino');
} catch (err) {
  pino = null;
}

function createLogger() {
  if (pino) {
    return pino({
      level: process.env.LOG_LEVEL || 'info',
      base: { service: 'statement-export-service' },
    });
  }
  // Fallback logger when pino is not installed (keeps unit tests dependency-free).
  const emit = (level) => (obj, msg) => {
    const line = typeof obj === 'string' ? obj : JSON.stringify(obj);
    // eslint-disable-next-line no-console
    console.log(`[${level}] ${msg || ''} ${line}`.trim());
  };
  return {
    info: emit('info'),
    warn: emit('warn'),
    error: emit('error'),
    debug: emit('debug'),
    child() {
      return this;
    },
  };
}

const logger = createLogger();

module.exports = { logger };
