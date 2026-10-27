'use strict';

const express = require('express');
const { authenticate } = require('../../middleware/auth');
const contactProfileService = require('../services/contactProfileService');
const statementUsageTracker = require('../services/statementUsageTracker');
const { maskEmail } = require('../utils/contactMask');
const { logger } = require('../../utils/logger');

const router = express.Router();

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const CHANNELS = new Set(['email', 'postal', 'portal']);

/**
 * Change only the address statements are emailed to. Support asked for the
 * old and new addresses in the log so they can confirm a customer's "I never
 * got my statement" report against the change history; both are masked.
 */
router.post('/email', authenticate(), (req, res) => {
  const email = req.body && typeof req.body.email === 'string' ? req.body.email.trim() : '';
  if (!EMAIL_PATTERN.test(email)) {
    return res.status(400).json({ error: 'BadRequest', message: 'enter a valid email address' });
  }
  const previous = contactProfileService.get(req.user.id);
  const profile = contactProfileService.updateEmail(req.user.id, email);

  logger.info(
    {
      userId: req.user.id,
      previousEmail: maskEmail(previous && previous.email),
      email: maskEmail(profile.email),
    },
    'delivery.email.changed'
  );

  return res.json({ status: 'pending-verification' });
});

/**
 * Change the delivery channel for future statements.
 */
router.post('/channel', authenticate(), (req, res) => {
  const channel = req.body && req.body.channel;
  if (!CHANNELS.has(channel)) {
    return res.status(400).json({ error: 'BadRequest', message: `unsupported channel: ${channel}` });
  }
  const profile = contactProfileService.get(req.user.id);
  if (channel === 'email' && !(profile && profile.email)) {
    return res
      .status(409)
      .json({ error: 'Conflict', message: 'add an email address before choosing email delivery' });
  }
  contactProfileService.update(req.user.id, { channel });
  statementUsageTracker.deliveryChannelChanged(req.user, channel);
  logger.info({ userId: req.user.id, channel }, 'delivery.channel.changed');
  return res.json({ status: 'updated', channel });
});

module.exports = router;
