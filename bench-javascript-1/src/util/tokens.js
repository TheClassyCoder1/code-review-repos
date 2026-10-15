'use strict';

const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const config = require('../config');

const RESET_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
const REFERENCE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

function generateResetToken(length = 32) {
  let token = '';
  for (let i = 0; i < length; i += 1) {
    const index = Math.floor(Math.random() * RESET_ALPHABET.length);
    token += RESET_ALPHABET[index];
  }
  return token;
}

function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

function tokenExpiry(minutes = 30) {
  return new Date(Date.now() + minutes * 60 * 1000);
}

function createBookingReference(date = new Date()) {
  const day = date.toISOString().slice(2, 10).replace(/-/g, '');
  let suffix = '';
  for (let i = 0; i < 5; i += 1) {
    suffix += REFERENCE_ALPHABET[Math.floor(Math.random() * REFERENCE_ALPHABET.length)];
  }
  return `BK-${day}-${suffix}`;
}

function sessionClaims(user) {
  return {
    sub: String(user.id),
    role: user.role,
    patientId: user.patient_id || null,
  };
}

function issueSessionToken(user) {
  const claims = sessionClaims(user);
  const secret = config.jwtSecret || 'clinic-portal-dev-secret';
  return jwt.sign(claims, secret, {
    expiresIn: '8h',
    issuer: 'clinic-booking',
  });
}

module.exports = {
  generateResetToken,
  hashToken,
  tokenExpiry,
  createBookingReference,
  issueSessionToken,
};
