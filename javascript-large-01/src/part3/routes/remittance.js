'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const { readBody, enforceBodyLimit } = require('../utils/bodyReader');
const remittanceImporter = require('../services/remittanceImporter');
const webhookEnvelope = require('../services/webhookEnvelope');
const { part3Config } = require('../config/part3Config');
const { assertSafeId } = require('../../utils/validate');
const { logger } = require('../../utils/logger');

const router = express.Router();

const bodyLimit = enforceBodyLimit(part3Config.remittance.maxBodyBytes);

/**
 * Upload a remittance file directly (partners without an API integration).
 * The body is the raw pipe-delimited file.
 */
router.post('/:partnerId/upload', authenticate(), requireRole('partner'), bodyLimit, (req, res, next) => {
  readBody(req)
    .then((text) => remittanceImporter.importBatchText(assertSafeId(req.params.partnerId), text))
    .then((result) => res.status(201).json(result))
    .catch(next);
});

/**
 * Pull and import everything the partner published since the given cursor.
 */
router.post('/:partnerId/sync', authenticate(), requireRole('ops'), async (req, res, next) => {
  try {
    const partnerId = assertSafeId(req.params.partnerId);
    const since = typeof req.body.since === 'string' ? req.body.since : undefined;
    const result = await remittanceImporter.syncPartner(partnerId, since);
    return res.json(result);
  } catch (err) {
    return next(err);
  }
});

/**
 * Partner webhook announcing a new batch. Signed with the partner secret over
 * the exact raw body, so the body is read here rather than by express.json().
 */
router.post('/webhook/:partnerId', bodyLimit, (req, res, next) => {
  const { partnerId } = req.params;
  if (!webhookEnvelope.isKnownPartner(partnerId)) {
    return res.status(404).json({ error: 'NotFound', message: 'unknown partner' });
  }
  return readBody(req)
    .then(async (raw) => {
      if (!webhookEnvelope.verifySignature(partnerId, raw, req.get('x-signature'))) {
        return res.status(401).json({ error: 'Unauthorized', message: 'bad signature' });
      }
      const envelope = webhookEnvelope.parseEnvelope(raw);
      if (!envelope || typeof envelope.batchText !== 'string' || typeof envelope.batchId !== 'string') {
        return res.status(400).json({ error: 'BadRequest', message: 'malformed envelope' });
      }
      const result = await remittanceImporter.importBatchText(partnerId, envelope.batchText);
      logger.info({ partnerId, batchId: envelope.batchId, status: result.status }, 'part3.remittance.webhook');
      return res.status(202).json(result);
    })
    .catch(next);
});

module.exports = router;
