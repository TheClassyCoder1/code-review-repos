'use strict';

/**
 * Cursor pagination helpers for the delivery history endpoints. Cursors are
 * opaque base64url strings wrapping the last id returned, so clients cannot
 * depend on their structure.
 */

const { ValidationError } = require('../../utils/validate');

const DEFAULT_LIMITS = Object.freeze({ defaultLimit: 25, maxLimit: 100 });

function encodeCursor(lastId) {
  return Buffer.from(JSON.stringify({ after: lastId }), 'utf8').toString('base64url');
}

function decodeCursor(cursor) {
  try {
    const parsed = JSON.parse(Buffer.from(cursor, 'base64url').toString('utf8'));
    if (!parsed || typeof parsed.after !== 'string' || parsed.after.length === 0) {
      throw new Error('missing position');
    }
    return parsed.after;
  } catch (err) {
    throw new ValidationError('invalid pagination cursor');
  }
}

/**
 * Parse `limit` and `cursor` from a query string object.
 *
 * `limit` defaults to defaultLimit, must be a positive integer, and is clamped
 * to maxLimit. A missing or empty cursor means "start from the beginning".
 */
function parsePageParams(query, limits = DEFAULT_LIMITS) {
  const rawLimit = query && query.limit;
  let limit = limits.defaultLimit;
  if (rawLimit !== undefined && rawLimit !== '') {
    if (!/^[0-9]{1,6}$/.test(String(rawLimit))) {
      throw new ValidationError('limit must be a positive integer');
    }
    limit = Number(rawLimit);
    if (limit < 1) {
      throw new ValidationError('limit must be a positive integer');
    }
    limit = Math.min(limit, limits.maxLimit);
  }

  const rawCursor = query && query.cursor;
  const after = typeof rawCursor === 'string' && rawCursor !== '' ? decodeCursor(rawCursor) : null;
  return { limit, after };
}

/**
 * Slice a list of rows (already sorted by ascending id) into a single page.
 * Fetches one extra row to tell whether another page exists without a count.
 */
function paginateById(rows, { limit, after }) {
  const sorted = [...rows].sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));
  const start = after === null ? 0 : sorted.findIndex((r) => r.id > after);
  if (start === -1) {
    return { items: [], nextCursor: null };
  }

  const window = sorted.slice(start, start + limit + 1);
  const hasMore = window.length > limit;
  const items = hasMore ? window.slice(0, limit) : window;
  return {
    items,
    nextCursor: hasMore ? encodeCursor(items[items.length - 1].id) : null,
  };
}

module.exports = { parsePageParams, paginateById, encodeCursor, decodeCursor, DEFAULT_LIMITS };
