'use strict';

/**
 * Raw body helpers for endpoints that receive non-JSON payloads (remittance
 * files, signed webhook envelopes) and therefore bypass express.json().
 */

/**
 * Buffer the request body and resolve it as a UTF-8 string.
 */
function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

class PayloadTooLargeError extends Error {
  constructor(limit) {
    super(`request body exceeds ${limit} bytes`);
    this.name = 'PayloadTooLargeError';
    this.status = 413;
  }
}

/**
 * Middleware that rejects requests whose declared or actual body size exceeds
 * maxBytes. The byte counter runs alongside any downstream reader and destroys
 * the request as soon as the limit is crossed, which makes readBody reject.
 */
function enforceBodyLimit(maxBytes) {
  return function limitBody(req, res, next) {
    const declared = Number(req.headers['content-length']);
    if (Number.isFinite(declared) && declared > maxBytes) {
      return next(new PayloadTooLargeError(maxBytes));
    }
    let seen = 0;
    req.on('data', (chunk) => {
      seen += chunk.length;
      if (seen > maxBytes) {
        req.destroy(new PayloadTooLargeError(maxBytes));
      }
    });
    return next();
  };
}

module.exports = { readBody, enforceBodyLimit, PayloadTooLargeError };
