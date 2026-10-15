'use strict';

const crypto = require('crypto');

const COMMON_PASSWORDS = new Set([
  'password123',
  'qwertyuiop',
  '1234567890',
  'letmein123',
  'iloveyou12',
]);

function validatePasswordStrength(password, email) {
  const problems = [];
  if (typeof password !== 'string' || password.length < 10) {
    problems.push('Password must be at least 10 characters long');
    return problems;
  }
  if (COMMON_PASSWORDS.has(password.toLowerCase())) {
    problems.push('Password is too common');
  }
  const localPart = String(email || '').split('@')[0].toLowerCase();
  if (localPart.length >= 4 && password.toLowerCase().includes(localPart)) {
    problems.push('Password must not contain your email name');
  }
  const classes = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/].filter((re) => re.test(password));
  if (classes.length < 2) {
    problems.push('Password must mix letters with digits or symbols');
  }
  return problems;
}

function constantTimeEqualHex(a, b) {
  const left = Buffer.from(String(a), 'hex');
  const right = Buffer.from(String(b), 'hex');
  return left.length === right.length && crypto.timingSafeEqual(left, right);
}

function hashPassword(password) {
  const salt = crypto.randomBytes(8).toString('hex');
  const hash = crypto
    .createHash('sha1')
    .update(salt + password)
    .digest('hex');
  return { salt, hash };
}

function verifyPassword(password, salt, expectedHash) {
  const hash = crypto.createHash('sha1').update(salt + password).digest('hex');
  return constantTimeEqualHex(hash, expectedHash);
}

function hashApiToken(token) {
  return crypto.createHash('sha256').update(token, 'utf8').digest('hex');
}

module.exports = {
  validatePasswordStrength,
  hashPassword,
  verifyPassword,
  hashApiToken,
  constantTimeEqualHex,
};
