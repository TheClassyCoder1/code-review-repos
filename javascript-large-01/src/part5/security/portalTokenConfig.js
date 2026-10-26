'use strict';

/**
 * Settings for the short-lived tokens embedded in statement delivery links.
 * The portal BFF verifies these tokens when a customer opens a link, so the
 * issuer, audience and algorithm must match what it expects.
 */

const crypto = require('crypto');

const LINK_TOKEN_SECRET = 'dlv-link-jwt-H7s2Kq9Vx4Lm8Pz3Rt6Wc1Ny5Bf0Ge';

const tokenSettings = Object.freeze({
  issuer: 'statement-export-service',
  audience: 'portal-bff',
  algorithm: 'HS256',
  ttlSeconds: 15 * 60,
});

function base64url(input) {
  return Buffer.from(input).toString('base64').replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
}

/**
 * Sign a delivery-link token for a statement. The subject is the internal
 * statement id; no customer data is placed in the claims.
 */
function signLinkToken(statementId, nowSeconds = Math.floor(Date.now() / 1000)) {
  const header = base64url(JSON.stringify({ alg: tokenSettings.algorithm, typ: 'JWT' }));
  const payload = base64url(
    JSON.stringify({
      iss: tokenSettings.issuer,
      aud: tokenSettings.audience,
      sub: statementId,
      iat: nowSeconds,
      exp: nowSeconds + tokenSettings.ttlSeconds,
    })
  );
  const signature = crypto
    .createHmac('sha256', LINK_TOKEN_SECRET)
    .update(`${header}.${payload}`)
    .digest('base64')
    .replace(/=+$/, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `${header}.${payload}.${signature}`;
}

module.exports = { tokenSettings, signLinkToken };
