'use strict';

const crypto = require('crypto');
const tokenStore = require('../models/agentTokenStore');
const { logger } = require('../../utils/logger');

/**
 * Issues and verifies field-agent device tokens.
 *
 * Tokens are never chosen by a person: the secret half is 32 bytes from the
 * CSPRNG (256 bits of entropy), so a single fast digest is sufficient for
 * storage — there is no dictionary to attack. Presented tokens that do not
 * have the exact issued shape are rejected before any lookup.
 */

const TOKEN_PREFIX = 'fca';
const TOKEN_SHAPE = /^fca_([a-f0-9]{16})_([A-Za-z0-9_-]{43})$/;
const TOKEN_TTL_MS = 1000 * 60 * 60 * 24 * 30;

function issue(agentId) {
  const tokenId = crypto.randomBytes(8).toString('hex');
  const secret = crypto.randomBytes(32).toString('base64url');
  const expiresAt = Date.now() + TOKEN_TTL_MS;
  tokenStore.saveToken({ tokenId, agentId, secret, expiresAt });
  logger.info({ agentId, tokenId }, 'part2.agent_token.issued');
  return { token: `${TOKEN_PREFIX}_${tokenId}_${secret}`, expiresAt };
}

/**
 * Resolve a presented bearer value to an agent id, or null.
 */
function verify(presented) {
  const match = typeof presented === 'string' ? TOKEN_SHAPE.exec(presented) : null;
  if (!match) {
    return null;
  }
  const [, tokenId, secret] = match;
  const record = tokenStore.findToken(tokenId);
  if (!record || record.revoked || record.expiresAt < Date.now()) {
    return null;
  }
  return tokenStore.matches(record, secret) ? record.agentId : null;
}

module.exports = { issue, verify, TOKEN_SHAPE };
