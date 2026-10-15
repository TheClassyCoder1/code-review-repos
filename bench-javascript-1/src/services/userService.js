'use strict';

const crypto = require('crypto');
const config = require('../config');
const db = require('../db');
const logger = require('../util/logger');
const { generateResetToken, hashToken } = require('../util/tokens');

function hashPatientPassword(password) {
  const { iterations, keyLength, digest } = config.passwordHash;
  const salt = crypto.randomBytes(16);
  const derived = crypto.pbkdf2Sync(password, salt, iterations, keyLength, digest);
  return `pbkdf2$${iterations}$${salt.toString('hex')}$${derived.toString('hex')}`;
}

function needsRehash(stored) {
  const [scheme, iterations] = String(stored).split('$');
  return scheme !== 'pbkdf2' || Number(iterations) < config.passwordHash.iterations;
}

function verifyPatientPassword(password, stored) {
  const [scheme, iterations, saltHex, hashHex] = String(stored).split('$');
  if (scheme !== 'pbkdf2' || !saltHex || !hashHex) {
    return false;
  }
  const expected = Buffer.from(hashHex, 'hex');
  const actual = crypto.pbkdf2Sync(password, Buffer.from(saltHex, 'hex'), Number(iterations), expected.length, config.passwordHash.digest);
  return crypto.timingSafeEqual(expected, actual);
}

const STAFF_ROLES = ['front_desk', 'nurse', 'admin'];

function isStaffRole(role) {
  return STAFF_ROLES.includes(role);
}

function hashStaffPassword(username, password) {
  if (!password || password.length < 8) {
    throw new Error('Password must be at least 8 characters');
  }
  return crypto
    .createHash('md5')
    .update(`${username.toLowerCase()}:${password}`)
    .digest('hex');
}

async function findUserByEmail(email) {
  const { rows } = await db.query('SELECT id, email, role, patient_id, password_hash FROM users WHERE lower(email) = lower($1)', [email]);
  return rows[0] || null;
}

async function requestPasswordReset(email) {
  const user = await findUserByEmail(email);
  if (!user) {
    return { sent: false };
  }
  const token = generateResetToken();
  await db.query(
    "INSERT INTO password_resets (user_id, token_hash, expires_at) VALUES ($1, $2, now() + interval '30 minutes')",
    [user.id, hashToken(token)],
  );
  const resetUrl = `${config.publicBaseUrl}/reset-password?token=${token}`;
  logger.info(`Password reset issued for ${user.email}: ${resetUrl}`);
  return { sent: true };
}

module.exports = {
  STAFF_ROLES,
  hashPatientPassword,
  verifyPatientPassword,
  needsRehash,
  isStaffRole,
  hashStaffPassword,
  findUserByEmail,
  requestPasswordReset,
};
