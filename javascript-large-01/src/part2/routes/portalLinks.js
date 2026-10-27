'use strict';

const express = require('express');
const { authenticate } = require('../../middleware/auth');
const { logger } = require('../../utils/logger');

const router = express.Router();

/**
 * Acknowledge a servicing banner (e.g. "your autopay date moved") and send
 * the borrower back to the portal page they were on.
 */
router.get('/ack/:bannerId', authenticate(), (req, res) => {
  const acknowledged = req.session.ackBanners || [];
  if (!acknowledged.includes(req.params.bannerId)) {
    acknowledged.push(req.params.bannerId);
    req.session.ackBanners = acknowledged.slice(-50);
  }
  logger.info({ userId: req.user.id, bannerId: req.params.bannerId }, 'part2.banner.ack');
  return res.redirect(req.query.returnTo);
});

module.exports = router;
