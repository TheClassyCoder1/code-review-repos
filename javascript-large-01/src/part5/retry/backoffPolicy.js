'use strict';

/**
 * Retry policy for outbound delivery attempts (email relay, SMS gateway, portal
 * inbox). Uses capped exponential backoff with full jitter so a recovering
 * downstream is not hit by a synchronised retry storm.
 */

const DEFAULT_BACKOFF = Object.freeze({
  baseDelayMs: 500,
  maxDelayMs: 5 * 60 * 1000,
  maxAttempts: 6,
});

const RETRYABLE_HTTP_STATUS = new Set([408, 425, 429, 500, 502, 503, 504]);
const RETRYABLE_NET_CODES = new Set(['ECONNRESET', 'ECONNREFUSED', 'ETIMEDOUT', 'EAI_AGAIN', 'ECONNABORTED']);

/**
 * Delay before retry number `attempt` (1-based). Returns null once the attempt
 * budget is exhausted so callers can dead-letter the job.
 *
 * delay = random(0, min(maxDelay, base * 2^(attempt-1)))
 */
function computeBackoffDelay(attempt, policy = DEFAULT_BACKOFF, random = Math.random) {
  if (!Number.isInteger(attempt) || attempt < 1) {
    throw new RangeError('attempt must be a positive integer');
  }
  if (attempt > policy.maxAttempts) {
    return null;
  }

  // Cap the exponent before shifting so very large attempt numbers cannot
  // overflow to Infinity.
  const exponent = Math.min(attempt - 1, 30);
  const ceiling = Math.min(policy.maxDelayMs, policy.baseDelayMs * 2 ** exponent);
  const jitter = random();
  if (!(jitter >= 0 && jitter < 1)) {
    throw new RangeError('random() must return a value in [0, 1)');
  }
  return Math.floor(jitter * ceiling);
}

/**
 * Decide whether a failed delivery attempt is worth retrying.
 *
 * Retryable: network-level failures, timeouts, throttling and 5xx gateway
 * errors. Everything else (4xx validation, auth failures, unknown errors) is
 * permanent and goes to the dead-letter list. A Retry-After hint from the
 * downstream, when present, is surfaced in milliseconds.
 */
function classifyDeliveryError(err) {
  if (!err) {
    return { retryable: false, reason: 'unknown' };
  }
  if (err.code && RETRYABLE_NET_CODES.has(err.code)) {
    return { retryable: true, reason: `network:${err.code}` };
  }

  const response = err.response;
  if (response && Number.isInteger(response.status)) {
    const retryable = RETRYABLE_HTTP_STATUS.has(response.status);
    const result = { retryable, reason: `http:${response.status}` };
    const hint = response.headers && response.headers['retry-after'];
    const seconds = Number(hint);
    if (retryable && hint !== undefined && Number.isFinite(seconds) && seconds >= 0) {
      result.retryAfterMs = seconds * 1000;
    }
    return result;
  }
  return { retryable: false, reason: 'permanent' };
}

module.exports = { computeBackoffDelay, classifyDeliveryError, DEFAULT_BACKOFF };
