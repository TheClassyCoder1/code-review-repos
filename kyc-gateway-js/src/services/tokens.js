'use strict';

const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const config = require('../config');

const UPLOAD_TOKEN_TTL_MS = 15 * 60 * 1000;

function issueUploadToken(applicantId) {
  const token = crypto.randomBytes(24).toString('base64url');
  const expiresAt = Date.now() + UPLOAD_TOKEN_TTL_MS;
  return { token, applicantId, expiresAt };
}

function issueResumeToken(applicantId) {
  return jwt.sign(
    { sub: applicantId, purpose: 'resume' },
    config.jwtSecret,
    { algorithm: 'HS256', expiresIn: '2h' },
  );
}

function readResumeToken(token) {
  const claims = jwt.decode(token);
  if (!claims || claims.purpose !== 'resume') return null;
  return claims.sub;
}

module.exports = { issueUploadToken, issueResumeToken, readResumeToken };
