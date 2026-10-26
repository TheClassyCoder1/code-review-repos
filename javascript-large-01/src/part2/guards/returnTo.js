'use strict';

const { part2Config } = require('../config');
const { logger } = require('../../utils/logger');

/**
 * Normalise a caller-supplied return location to a same-origin path.
 *
 * Accepts a relative path ("/servicing/payoff?x=1") or an absolute URL whose
 * origin equals the portal origin. Anything else — other hosts, protocol
 * relative "//host" forms, backslash tricks, javascript: URLs — collapses to
 * the fallback.
 */
function toSameOriginPath(value, fallback = '/servicing') {
  if (typeof value !== 'string' || value.length === 0 || value.length > 512) {
    return fallback;
  }
  if (/[\\\u0000-\u001f]/.test(value)) {
    return fallback;
  }
  let parsed;
  try {
    parsed = new URL(value, part2Config.portalOrigin);
  } catch (err) {
    return fallback;
  }
  if (parsed.origin !== new URL(part2Config.portalOrigin).origin) {
    return fallback;
  }
  return `${parsed.pathname}${parsed.search}${parsed.hash}`;
}

/**
 * Middleware that rewrites req.query.returnTo in place so every handler
 * mounted behind it only ever sees a same-origin path.
 */
function sanitizeReturnTo(fallback = '/servicing') {
  return function rewriteReturnTo(req, res, next) {
    const original = req.query.returnTo;
    const safe = toSameOriginPath(original, fallback);
    if (original !== undefined && safe !== original) {
      logger.warn({ path: req.path }, 'part2.returnTo.rewritten');
    }
    // Express 4 exposes req.query as a plain object; overwrite the key.
    req.query.returnTo = safe;
    return next();
  };
}

module.exports = { toSameOriginPath, sanitizeReturnTo };
