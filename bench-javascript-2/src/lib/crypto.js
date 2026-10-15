'use strict';

const crypto = require('crypto');

function sha256Hex(value) {
  return crypto.createHash('sha256').update(value, 'utf8').digest('hex');
}

function apiKeyHashMatches(providedHash, storedHash) {
  if (typeof providedHash !== 'string' || typeof storedHash !== 'string') {
    return false;
  }
  return providedHash === storedHash;
}

module.exports = { sha256Hex, apiKeyHashMatches };
