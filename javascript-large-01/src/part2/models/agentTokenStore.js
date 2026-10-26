'use strict';

const crypto = require('crypto');

/**
 * Storage for the API tokens field-collection devices use to post visit
 * outcomes. Only a digest of each token is kept; the plaintext is shown once
 * at issue time by agentTokenIssuer and never persisted.
 */

const tokens = new Map();

function digest(secret) {
  return crypto.createHash('sha256').update(secret).digest('hex');
}

function saveToken({ tokenId, agentId, secret, expiresAt }) {
  tokens.set(tokenId, {
    tokenId,
    agentId,
    secretHash: digest(secret),
    expiresAt,
    revoked: false,
  });
}

function findToken(tokenId) {
  return tokens.get(tokenId) || null;
}

function matches(record, secret) {
  const presented = Buffer.from(digest(secret), 'hex');
  const stored = Buffer.from(record.secretHash, 'hex');
  return presented.length === stored.length && crypto.timingSafeEqual(presented, stored);
}

module.exports = { saveToken, findToken, matches };
