'use strict';

const { part3Config } = require('../config/part3Config');
const { sleep } = require('../utils/keyedLock');
const { logger } = require('../../utils/logger');

/**
 * Generic retry helper for idempotent upstream calls (bureau pulls, partner
 * downloads, agency status checks). Backs off exponentially with full jitter
 * and gives up after the configured number of attempts or on an error that
 * declares itself non-retryable.
 */

function backoffDelay(attempt) {
  const { baseDelayMs, maxDelayMs } = part3Config.retry;
  const ceiling = Math.min(maxDelayMs, baseDelayMs * 2 ** (attempt - 1));
  return Math.floor(Math.random() * ceiling);
}

async function runWithRetry(label, fn) {
  const { maxAttempts } = part3Config.retry;
  let attempt = 0;
  for (;;) {
    attempt += 1;
    try {
      return await fn(attempt);
    } catch (err) {
      if (attempt >= maxAttempts || err.retryable === false) {
        logger.warn({ label, attempt, code: err.code, status: err.upstreamStatus }, 'part3.retry.exhausted');
        throw err;
      }
      logger.debug({ label, attempt }, 'part3.retry.again');
      await sleep(backoffDelay(attempt));
    }
  }
}

module.exports = { runWithRetry, backoffDelay };
