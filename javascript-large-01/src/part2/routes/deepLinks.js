'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const deepLinks = require('../services/deepLinkService');
const { logger } = require('../../utils/logger');

const router = express.Router();

router.post('/', authenticate(), requireRole('servicing:write'), (req, res, next) => {
  try {
    const link = deepLinks.create({
      target: req.body && req.body.target,
      campaign: req.body && req.body.campaign,
      createdBy: req.user.id,
    });
    return res.status(201).json({ code: link.code, target: link.target });
  } catch (err) {
    return next(err);
  }
});

/**
 * Public resolver for printed short links.
 */
router.get('/:code', (req, res) => {
  const link = deepLinks.resolve(req.params.code);
  if (!link) {
    return res.status(404).json({ error: 'NotFound', message: 'unknown link' });
  }
  logger.info({ code: link.code, campaign: link.campaign }, 'part2.deeplink.hit');
  return res.redirect(302, link.target);
});

module.exports = router;
