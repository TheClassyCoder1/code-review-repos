'use strict';

const express = require('express');
const { authenticate } = require('../../middleware/auth');
const kycService = require('../services/kycService');

const router = express.Router();

const NAME_PATTERN = /^[\p{L}][\p{L} .'-]{0,99}$/u;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const PHONE_PATTERN = /^\+?[0-9 ()-]{7,20}$/;
const POSTCODE_PATTERN = /^[A-Za-z0-9 -]{3,10}$/;

/**
 * Current verification status for the caller.
 */
router.get('/status', authenticate(), (req, res) => {
  res.json(kycService.status(req.user.id));
});

/**
 * Submit identity details for verification. The portal sends the same
 * personal details as the contact profile form plus the national identifier.
 */
router.post('/verify', authenticate(), async (req, res, next) => {
  const body = req.body || {};
  const errors = [];
  if (typeof body.fullName !== 'string' || !NAME_PATTERN.test(body.fullName.trim())) {
    errors.push({ field: 'fullName', message: 'enter your full legal name' });
  }
  if (typeof body.email !== 'string' || !EMAIL_PATTERN.test(body.email.trim())) {
    errors.push({ field: 'email', message: 'enter a valid email address' });
  }
  if (body.phone !== undefined && body.phone !== null && body.phone !== '') {
    if (typeof body.phone !== 'string' || !PHONE_PATTERN.test(body.phone.trim())) {
      errors.push({ field: 'phone', message: 'enter a valid phone number' });
    }
  }
  if (typeof body.postcode !== 'string' || !POSTCODE_PATTERN.test(body.postcode.trim())) {
    errors.push({ field: 'postcode', message: 'enter a valid postcode' });
  }
  if (typeof body.dateOfBirth !== 'string' || Number.isNaN(Date.parse(body.dateOfBirth))) {
    errors.push({ field: 'dateOfBirth', message: 'enter your date of birth as YYYY-MM-DD' });
  } else if (Date.parse(body.dateOfBirth) > Date.now()) {
    errors.push({ field: 'dateOfBirth', message: 'date of birth cannot be in the future' });
  }
  if (typeof body.nationalId !== 'string' || body.nationalId.trim() === '') {
    errors.push({ field: 'nationalId', message: 'enter your national identification number' });
  }
  if (typeof body.country !== 'string' || !/^[A-Za-z]{2}$/.test(body.country)) {
    errors.push({ field: 'country', message: 'select the country that issued your identifier' });
  }
  if (errors.length > 0) {
    return res.status(400).json({ error: 'ValidationFailed', errors });
  }

  try {
    const result = await kycService.verifyIdentity(req.user.id, {
      fullName: body.fullName.trim(),
      email: body.email.trim(),
      phone: body.phone ? body.phone.trim() : null,
      postcode: body.postcode.trim().toUpperCase(),
      dateOfBirth: body.dateOfBirth,
      nationalId: body.nationalId,
      country: body.country,
    });
    return res.status(201).json(result);
  } catch (err) {
    if (err instanceof kycService.KycError) {
      return res.status(err.status).json({ error: 'VerificationFailed', message: err.message });
    }
    return next(err);
  }
});

module.exports = router;
