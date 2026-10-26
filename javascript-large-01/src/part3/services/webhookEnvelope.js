'use strict';

const crypto = require('crypto');

/**
 * Partner webhook envelopes announcing that a remittance batch is ready.
 * Envelopes are signed with a per-partner shared secret.
 */

const PARTNER_SECRETS = {
  'partner-payroll-01': process.env.P3_PARTNER_PAYROLL_SECRET || 'payroll-hook-local-2024',
  'partner-agency-02': process.env.P3_PARTNER_AGENCY_SECRET || 'agency-hook-local-2024',
};

/**
 * Parse an envelope body. Malformed JSON yields undefined so the caller can
 * answer 400 without an exception path.
 */
function parseEnvelope(raw) {
  try {
    return JSON.parse(raw);
  } catch (err) {
    return undefined;
  }
}

function verifySignature(partnerId, raw, signatureHex) {
  const secret = PARTNER_SECRETS[partnerId];
  if (!secret || typeof signatureHex !== 'string' || !/^[0-9a-f]{64}$/.test(signatureHex)) {
    return false;
  }
  const expected = crypto.createHmac('sha256', secret).update(raw).digest();
  return crypto.timingSafeEqual(expected, Buffer.from(signatureHex, 'hex'));
}

function isKnownPartner(partnerId) {
  return Object.prototype.hasOwnProperty.call(PARTNER_SECRETS, partnerId);
}

module.exports = { parseEnvelope, verifySignature, isKnownPartner };
