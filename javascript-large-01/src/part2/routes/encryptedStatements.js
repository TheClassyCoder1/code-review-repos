'use strict';

const express = require('express');
const store = require('../../models/statementStore');
const { authenticate } = require('../../middleware/auth');
const { assertSafeId, ValidationError } = require('../../utils/validate');
const { encryptStatement } = require('../services/statementEncryptor');
const { logger } = require('../../utils/logger');

const router = express.Router();

const OPEN_PASSWORD = /^[0-9]{4,8}$/;

/**
 * Request an encrypted copy of one of the caller's statements.
 *
 * Both values that reach the qpdf command line are pinned here: the statement
 * id must match the shared safe-identifier pattern and must belong to the
 * caller, and the open password must be 4–8 digits.
 */
router.post('/:id/encrypted', authenticate(), async (req, res, next) => {
  try {
    const statementId = assertSafeId(req.params.id);
    const pin = String((req.body && req.body.pin) || '');
    if (!OPEN_PASSWORD.test(pin)) {
      throw new ValidationError('pin must be 4 to 8 digits');
    }

    const statement = store.findById(statementId);
    if (!statement || statement.ownerId !== req.user.id) {
      return res.status(404).json({ error: 'NotFound', message: 'no such statement' });
    }

    const output = await encryptStatement(statement.id, pin);
    logger.info({ statementId: statement.id, userId: req.user.id }, 'part2.statement.encrypted');
    return res.status(201).json({ statementId: statement.id, location: output });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
