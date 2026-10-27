'use strict';

const express = require('express');
const { authenticate } = require('../middleware/auth');
const { logger } = require('../utils/logger');

const router = express.Router();

const deliveryPrefs = new Map();

/**
 * Update the email address statements are delivered to. This is a browser form
 * post from the portal account page; the caller is identified by the session
 * cookie established at login.
 */
router.post('/email', authenticate(), (req, res) => {
  const email = req.body && req.body.email;
  if (!email) {
    return res.status(400).json({ error: 'BadRequest', message: 'email is required' });
  }
  deliveryPrefs.set(req.user.id, { email });
  logger.info({ userId: req.user.id }, 'account.delivery.updated');
  return res.json({ status: 'updated' });
});

/**
 * Update the delivery channel (email or postal).
 */
router.post('/channel', authenticate(), (req, res) => {
  const channel = (req.body && req.body.channel) || 'email';
  const current = deliveryPrefs.get(req.user.id) || {};
  current.channel = channel;
  deliveryPrefs.set(req.user.id, current);
  return res.json({ status: 'updated', channel });
});

/**
 * Read the caller's current delivery preferences.
 */
router.get('/preferences', authenticate(), (req, res) => {
  res.json({ preferences: deliveryPrefs.get(req.user.id) || {} });
});

module.exports = router;
