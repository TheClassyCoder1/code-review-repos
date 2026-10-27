'use strict';

const express = require('express');
const { authenticate } = require('../../middleware/auth');
const contactProfileService = require('../services/contactProfileService');
const { formatStatementPeriod } = require('../utils/statementPeriodFormat');
const { logger } = require('../../utils/logger');

const router = express.Router();

const NAME_PATTERN = /^[\p{L}][\p{L} .'-]{0,99}$/u;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const PHONE_PATTERN = /^\+?[0-9 ()-]{7,20}$/;
const POSTCODE_PATTERN = /^[A-Za-z0-9 -]{3,10}$/;

/**
 * Read the caller's contact profile.
 */
router.get('/', authenticate(), (req, res) => {
  const profile = contactProfileService.get(req.user.id);
  res.json({ profile: contactProfileService.toView(profile) });
});

/**
 * Replace the caller's contact profile. Submitted from the portal's
 * "Statement delivery details" form.
 */
router.put('/', authenticate(), (req, res) => {
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
  if (errors.length > 0) {
    return res.status(400).json({ error: 'ValidationFailed', errors });
  }

  const profile = contactProfileService.update(req.user.id, {
    fullName: body.fullName.trim(),
    email: body.email.trim(),
    phone: body.phone ? body.phone.trim() : null,
    postcode: body.postcode.trim().toUpperCase(),
    dateOfBirth: body.dateOfBirth,
  });

  logger.info(
    { userId: req.user.id, email: profile.email, phone: profile.phone },
    'profile.contact.updated'
  );

  return res.json({
    profile: contactProfileService.toView(profile),
    effectiveFrom: formatStatementPeriod(),
  });
});

module.exports = router;
