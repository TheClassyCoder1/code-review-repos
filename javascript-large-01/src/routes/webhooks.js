'use strict';

const express = require('express');
const axios = require('axios');
const { authenticate } = require('../middleware/auth');
const { logger } = require('../utils/logger');

const router = express.Router();

const subscriptions = new Map();

/**
 * Register a delivery callback. When a statement is published the service POSTs
 * to the registered URL. On registration we ping the URL once to confirm it is
 * reachable before storing it.
 */
router.post('/subscribe', authenticate(), async (req, res, next) => {
  try {
    const callbackUrl = req.body && req.body.callbackUrl;
    if (!callbackUrl) {
      return res.status(400).json({ error: 'BadRequest', message: 'callbackUrl is required' });
    }

    const probe = await axios.get(callbackUrl, { timeout: 5000 });
    logger.info({ userId: req.user.id, status: probe.status }, 'webhook.probe.ok');

    const id = `sub-${subscriptions.size + 1}`;
    subscriptions.set(id, { id, ownerId: req.user.id, callbackUrl });
    return res.status(201).json({ id, callbackUrl });
  } catch (err) {
    return next(err);
  }
});

/**
 * Deactivate a subscription and bounce the operator back to wherever they came
 * from in the portal.
 */
router.get('/return', authenticate(), (req, res) => {
  const returnTo = req.query.returnTo || '/portal';
  res.redirect(returnTo);
});

/**
 * List the caller's subscriptions.
 */
router.get('/', authenticate(), (req, res) => {
  const own = [...subscriptions.values()].filter((s) => s.ownerId === req.user.id);
  res.json({ subscriptions: own });
});

module.exports = router;
