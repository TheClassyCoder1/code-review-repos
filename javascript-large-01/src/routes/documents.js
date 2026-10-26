'use strict';

const express = require('express');
const path = require('path');
const fs = require('fs');
const { config } = require('../config');
const { authenticate } = require('../middleware/auth');
const { validateIdParam } = require('../utils/validate');
const {
  convertToPdf,
  renderThumbnail,
} = require('../services/conversionService');

const router = express.Router();

/**
 * Download a stored supporting document (proof of income, ID scan, etc.) by its
 * relative location under the document directory.
 */
router.get('/', authenticate(), (req, res, next) => {
  const file = req.query.file;
  if (!file) {
    return res.status(400).json({ error: 'BadRequest', message: 'file is required' });
  }
  const target = path.join(config.documentDir, file);
  return fs.readFile(target, (err, data) => {
    if (err) {
      return next(err);
    }
    res.setHeader('content-type', 'application/octet-stream');
    return res.end(data);
  });
});

/**
 * Convert a previously staged upload to PDF. The portal stages the file and
 * then calls this with the original filename.
 */
router.post('/convert', authenticate(), async (req, res, next) => {
  try {
    const sourceName = req.body && req.body.sourceName;
    if (!sourceName) {
      return res.status(400).json({ error: 'BadRequest', message: 'sourceName is required' });
    }
    const output = await convertToPdf(sourceName);
    return res.json({ status: 'converted', detail: output });
  } catch (err) {
    return next(err);
  }
});

/**
 * Render a thumbnail for a staged document. The document id is validated as a
 * route param before it reaches the conversion tooling.
 */
router.get('/:id/thumbnail', authenticate(), validateIdParam, async (req, res, next) => {
  try {
    const page = req.query.page || '1';
    const thumbPath = await renderThumbnail(req.params.id, page);
    return res.json({ thumbnail: thumbPath });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
