'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const registry = require('../services/callbackRegistry');

const router = express.Router();

/**
 * Partner callback registration. The authenticated principal is the partner
 * id that owns the registration.
 */
router.post('/', authenticate(), requireRole('partner'), async (req, res, next) => {
  try {
    const registration = await registry.register(req.user.id, req.body || {});
    return res.status(201).json({
      id: registration.id,
      url: registration.url,
      events: registration.events,
      secret: registration.secret,
    });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
