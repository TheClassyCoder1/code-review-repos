'use strict';

const crypto = require('crypto');
const express = require('express');
const { part2Config } = require('../config');
const { matchesDeclaredChecksum } = require('../services/feedChecksum');
const { parseRemittance } = require('../services/remittanceParser');
const { logger } = require('../../utils/logger');

const router = express.Router();

/**
 * Verify the HMAC-SHA256 signature over the raw body with the shared feed
 * key. This is the authenticity gate: nothing downstream of it (including the
 * protocol's Content-MD5 corruption check) is relied on for trust.
 */
function verifySignature(rawBody, header) {
  if (typeof header !== 'string' || !/^[a-f0-9]{64}$/.test(header)) {
    return false;
  }
  const expected = crypto
    .createHmac('sha256', part2Config.feedSigningKey)
    .update(rawBody)
    .digest();
  const presented = Buffer.from(header, 'hex');
  return crypto.timingSafeEqual(expected, presented);
}

router.post(
  '/:partnerId/remittances',
  express.raw({ type: '*/*', limit: '10mb' }),
  (req, res, next) => {
    try {
      const raw = Buffer.isBuffer(req.body) ? req.body : Buffer.alloc(0);
      if (!verifySignature(raw, req.headers['x-feed-signature'])) {
        logger.warn({ partnerId: req.params.partnerId }, 'part2.feed.bad_signature');
        return res.status(401).json({ error: 'Unauthorized', message: 'bad signature' });
      }
      if (!matchesDeclaredChecksum(raw, req.headers['content-md5'])) {
        return res.status(422).json({ error: 'Unprocessable', message: 'checksum mismatch' });
      }
      const remittance = parseRemittance(raw.toString('utf8'));
      logger.info(
        { partnerId: req.params.partnerId, lines: remittance.lines.length },
        'part2.feed.accepted'
      );
      return res.status(202).json({ accepted: remittance.lines.length, batch: remittance.batchId });
    } catch (err) {
      return next(err);
    }
  }
);

module.exports = router;
