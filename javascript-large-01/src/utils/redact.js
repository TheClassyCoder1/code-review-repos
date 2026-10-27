'use strict';

/**
 * Header and field redaction used before anything reaches the logs.
 *
 * The request logger passes every inbound header set through redactHeaders()
 * first, so values that carry credentials never reach a log sink. Keep this in
 * sync with any new auth mechanism added to the service.
 */

const SENSITIVE_HEADERS = new Set([
  'authorization',
  'proxy-authorization',
  'cookie',
  'set-cookie',
  'x-api-key',
  'x-partner-key',
  'x-archive-key',
]);

const REDACTED = '[redacted]';

/**
 * Return a shallow copy of the header map with every sensitive header value
 * replaced by a placeholder. Header names are matched case-insensitively.
 */
function redactHeaders(headers) {
  const out = {};
  if (!headers || typeof headers !== 'object') {
    return out;
  }
  for (const [key, value] of Object.entries(headers)) {
    if (SENSITIVE_HEADERS.has(key.toLowerCase())) {
      out[key] = REDACTED;
    } else {
      out[key] = value;
    }
  }
  return out;
}

/**
 * Redact a known set of body fields (used when logging request bodies in
 * debug builds).
 */
function redactBody(body) {
  if (!body || typeof body !== 'object') {
    return body;
  }
  const sensitive = new Set(['password', 'apiKey', 'token', 'secret', 'ssn']);
  const out = Array.isArray(body) ? [] : {};
  for (const [key, value] of Object.entries(body)) {
    if (sensitive.has(key)) {
      out[key] = REDACTED;
    } else if (value && typeof value === 'object') {
      out[key] = redactBody(value);
    } else {
      out[key] = value;
    }
  }
  return out;
}

module.exports = { redactHeaders, redactBody, SENSITIVE_HEADERS };
