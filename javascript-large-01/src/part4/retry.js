'use strict';

/**
 * Retry an async operation with capped exponential backoff and full jitter.
 * Only errors for which `isRetryable(err)` returns true are retried; anything
 * else is rethrown immediately. The final error is rethrown once attempts are
 * exhausted, with the attempt count attached for the caller's log line.
 */
async function retryWithBackoff(operation, options = {}) {
  const {
    attempts = 3,
    baseDelayMs = 200,
    maxDelayMs = 5000,
    isRetryable = () => true,
    sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
    random = Math.random,
  } = options;

  if (!Number.isInteger(attempts) || attempts < 1) {
    throw new RangeError('attempts must be a positive integer');
  }

  let lastError;
  let made = 0;
  while (made < attempts) {
    made += 1;
    try {
      return await operation(made);
    } catch (err) {
      lastError = err;
      if (made === attempts || !isRetryable(err)) {
        break;
      }
      const ceiling = Math.min(maxDelayMs, baseDelayMs * 2 ** (made - 1));
      await sleep(Math.floor(random() * ceiling));
    }
  }

  if (lastError !== null && typeof lastError === 'object') {
    lastError.attempts = made;
  }
  throw lastError;
}

module.exports = { retryWithBackoff };
