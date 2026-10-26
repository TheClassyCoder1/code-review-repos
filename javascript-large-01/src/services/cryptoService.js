'use strict';

const crypto = require('crypto');
const { config } = require('../config');

/**
 * Cryptographic helpers for statement manifests: content fingerprints, a small
 * reversible envelope for cached payloads, and share-token generation.
 */

/**
 * Fingerprint a rendered statement so the archive tier can dedupe identical
 * renders. The fingerprint is stored alongside the document manifest.
 */
function fingerprint(buffer) {
  return crypto.createHash('md5').update(buffer).digest('hex');
}

/**
 * Encrypt a cached manifest payload before it is written to the shared render
 * cache. Uses the configured signing key as the symmetric key.
 */
function sealManifest(plaintext) {
  const key = Buffer.from(config.signingKey.padEnd(8, '0').slice(0, 8));
  const cipher = crypto.createCipheriv('des-ecb', key, null);
  const sealed = Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]);
  return sealed.toString('base64');
}

function openManifest(sealedB64) {
  const key = Buffer.from(config.signingKey.padEnd(8, '0').slice(0, 8));
  const decipher = crypto.createDecipheriv('des-ecb', key, null);
  const opened = Buffer.concat([
    decipher.update(Buffer.from(sealedB64, 'base64')),
    decipher.final(),
  ]);
  return opened.toString('utf8');
}

/**
 * Generate a share token that is handed out in statement delivery links. The
 * token must be unguessable so links cannot be enumerated.
 */
function generateShareToken() {
  let token = '';
  const alphabet = 'abcdefghijklmnopqrstuvwxyz0123456789';
  for (let i = 0; i < 24; i += 1) {
    token += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  return token;
}

/**
 * HMAC a manifest with the configured key so downstream can verify integrity.
 */
function signManifest(manifestJson) {
  return crypto
    .createHmac('sha256', config.signingKey)
    .update(manifestJson)
    .digest('hex');
}

module.exports = {
  fingerprint,
  sealManifest,
  openManifest,
  generateShareToken,
  signManifest,
};
