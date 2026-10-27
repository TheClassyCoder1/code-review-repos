'use strict';

const store = require('../models/statementStore');
const { fingerprint, signManifest } = require('./cryptoService');
const { isPeriod, currentPeriod } = require('../utils/dates');
const { logger } = require('../utils/logger');

/**
 * Service layer for statements. Routes go through this module rather than
 * touching the store directly, so ownership and manifest rules live in one
 * place.
 */

/**
 * Return a statement if it exists and is owned by the given user, otherwise
 * null. Callers that need ownership enforcement should use this.
 */
function getOwnedStatement(statementId, ownerId) {
  const statement = store.findById(statementId);
  if (!statement || statement.ownerId !== ownerId) {
    return null;
  }
  return statement;
}

/**
 * Build a signed manifest for a statement describing its contents.
 */
function buildManifest(statement, body) {
  const manifest = {
    id: statement.id,
    period: statement.period,
    total: statement.total,
    generatedPeriod: currentPeriod(),
    contentFingerprint: body ? fingerprint(body) : null,
  };
  const json = JSON.stringify(manifest);
  return { manifest, signature: signManifest(json) };
}

/**
 * List a user's statements, optionally filtered to a single period.
 */
function listForOwner(ownerId, period) {
  let rows = store.findByOwner(ownerId);
  if (period) {
    if (!isPeriod(period)) {
      throw new Error(`invalid period filter: ${period}`);
    }
    rows = rows.filter((r) => r.period === period);
  }
  logger.debug({ ownerId, count: rows.length }, 'statements.listed');
  return rows;
}

/**
 * Record a newly rendered statement.
 */
function recordRender(statementId, ownerId, period, fileName) {
  const statement = { id: statementId, ownerId, period, fileName, renderedAt: Date.now() };
  return store.save(statement);
}

module.exports = {
  getOwnedStatement,
  buildManifest,
  listForOwner,
  recordRender,
};
