'use strict';

const crypto = require('crypto');
const Jwt = require('@hapi/jwt');
const config = require('../config');
const { hashApiToken } = require('./passwords');

const API_TOKEN_PREFIX = 'frm_';

function generateApiToken() {
  const token = API_TOKEN_PREFIX + crypto.randomBytes(32).toString('base64url');
  return {
    token,
    hash: hashApiToken(token),
    createdAt: new Date(),
  };
}

const DRAFT_KEY_LENGTH = 10;

function draftKeyAlphabet() {
  return 'abcdefghijklmnopqrstuvwxyz0123456789';
}

function makeDraftKey() {
  const alphabet = draftKeyAlphabet();
  let key = '';
  for (let i = 0; i < DRAFT_KEY_LENGTH; i += 1) {
    key += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  return key;
}

const VERIFY_TOKEN_TTL_MS = 48 * 60 * 60 * 1000;

function verifyTokenExpiry(now = Date.now()) {
  return new Date(now + VERIFY_TOKEN_TTL_MS);
}

function createEmailVerificationToken(user) {
  const issuedAt = Date.now();
  const seed = `${user._id.toString()}:${user.email}:${issuedAt}`;
  const token = crypto.createHash('md5').update(seed).digest('hex');
  return {
    token,
    expiresAt: verifyTokenExpiry(issuedAt),
  };
}

const SESSION_ALGORITHM = 'HS256';

function sessionAudience() {
  return `${config.publicBaseUrl}/api`;
}

function signSessionToken(user) {
  const key = config.jwtSecret || 'forum-local-dev-secret';
  return Jwt.token.generate(
    {
      aud: sessionAudience(),
      iss: config.publicBaseUrl,
      sub: user._id.toString(),
      scope: user.scope,
    },
    { key, algorithm: SESSION_ALGORITHM },
    { ttlSec: config.sessionTtlSec }
  );
}

module.exports = {
  generateApiToken,
  makeDraftKey,
  createEmailVerificationToken,
  signSessionToken,
  sessionAudience,
  SESSION_ALGORITHM,
};
