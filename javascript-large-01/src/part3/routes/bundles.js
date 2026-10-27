'use strict';

const express = require('express');
const { pipeline } = require('stream');
const { authenticate } = require('../../middleware/auth');
const statementService = require('../../services/statementService');
const statementBundle = require('../services/statementBundle');
const { assertSafeId } = require('../../utils/validate');
const { logger } = require('../../utils/logger');

const router = express.Router();

function ownedOr404(req, res) {
  const statement = statementService.getOwnedStatement(assertSafeId(req.params.id), req.user.id);
  if (!statement) {
    res.status(404).json({ error: 'NotFound', message: 'no such statement' });
    return null;
  }
  return statement;
}

/**
 * Stream the rendered PDF. pipeline() destroys the file stream when the
 * response finishes, errors, or the client disconnects mid-download.
 */
router.get('/:id/pdf', authenticate(), (req, res, next) => {
  try {
    const statement = ownedOr404(req, res);
    if (!statement) {
      return undefined;
    }
    const source = statementBundle.openStatementStream(statement.id);
    res.setHeader('content-type', 'application/pdf');
    return pipeline(source, res, (err) => {
      if (err && err.code !== 'ERR_STREAM_PREMATURE_CLOSE') {
        logger.warn({ statementId: statement.id, code: err.code }, 'part3.bundle.stream_failed');
        if (!res.headersSent) {
          next(err);
        }
      }
    });
  } catch (err) {
    return next(err);
  }
});

/**
 * Return the decoded manifest header for a statement bundle.
 */
router.get('/:id/manifest', authenticate(), async (req, res, next) => {
  let opened = null;
  try {
    const statement = ownedOr404(req, res);
    if (!statement) {
      return undefined;
    }
    opened = await statementBundle.openManifest(statement.id);
    const length = Math.min(opened.size, statementBundle.MANIFEST_HEADER_BYTES);
    const buffer = Buffer.alloc(length);
    await opened.handle.read(buffer, 0, length, 0);
    return res.json({ statementId: statement.id, ...statementBundle.decodeManifestHeader(buffer) });
  } catch (err) {
    return next(err);
  } finally {
    if (opened) {
      await opened.handle.close();
    }
  }
});

module.exports = router;
