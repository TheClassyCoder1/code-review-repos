'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const reminderDispatcher = require('../services/reminderDispatcher');
const { SUPPORTED_CHANNELS } = require('../services/reminderTemplates');
const { assertSafeId, ValidationError } = require('../../utils/validate');

const router = express.Router();

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Normalise and validate the requested channels against the template set.
 * Anything not in SUPPORTED_CHANNELS is rejected before rendering.
 */
function parseChannels(raw) {
  const list = Array.isArray(raw) ? raw : [raw];
  const channels = [...new Set(list.map((c) => String(c).toLowerCase()))];
  if (channels.length === 0) {
    throw new ValidationError('at least one channel is required');
  }
  for (const channel of channels) {
    if (!SUPPORTED_CHANNELS.includes(channel)) {
      throw new ValidationError(`unsupported channel: ${channel}`);
    }
  }
  return channels;
}

/**
 * Send a payment reminder for a loan. Triggered by the dunning scheduler.
 */
router.post('/:loanId', authenticate(), requireRole('ops'), async (req, res, next) => {
  try {
    const loanId = assertSafeId(req.params.loanId);
    const contactRef = assertSafeId(req.body.contactRef);
    const channels = parseChannels(req.body.channels);
    if (typeof req.body.dueDate !== 'string' || !ISO_DATE.test(req.body.dueDate)) {
      throw new ValidationError('dueDate must be YYYY-MM-DD');
    }
    const result = await reminderDispatcher.dispatch(loanId, contactRef, channels, req.body.dueDate);
    return res.status(202).json(result);
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
