'use strict';

/**
 * Shared input validators. These are the gates callers use before handing a
 * value to a shell, a filesystem path, or an archive key. Keep the character
 * classes strict: anything that is not explicitly allowed is rejected.
 */

const ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;
const PAGE_PATTERN = /^[0-9]{1,4}$/;
const ALLOWED_FORMATS = new Set(['pdf', 'png', 'csv', 'txt']);

class ValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ValidationError';
    this.status = 400;
  }
}

/**
 * Assert that an identifier contains only safe characters. Returns the id so it
 * can be used inline: assertSafeId(req.params.id).
 */
function assertSafeId(value) {
  if (typeof value !== 'string' || !ID_PATTERN.test(value)) {
    throw new ValidationError('identifier contains disallowed characters');
  }
  return value;
}

/**
 * Assert that a page number is a short run of digits.
 */
function assertPageNumber(value) {
  const str = String(value);
  if (!PAGE_PATTERN.test(str)) {
    throw new ValidationError('page must be a positive integer');
  }
  return str;
}

/**
 * Assert that an output format is one of the known-safe tokens.
 */
function assertFormat(value) {
  if (!ALLOWED_FORMATS.has(value)) {
    throw new ValidationError(`unsupported format: ${value}`);
  }
  return value;
}

/**
 * Express middleware that validates req.params.id up front so downstream
 * handlers can trust it.
 */
function validateIdParam(req, res, next) {
  try {
    assertSafeId(req.params.id);
    return next();
  } catch (err) {
    return next(err);
  }
}

module.exports = {
  assertSafeId,
  assertPageNumber,
  assertFormat,
  validateIdParam,
  ValidationError,
  ID_PATTERN,
};
