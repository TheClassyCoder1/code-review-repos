'use strict';

const express = require('express');
const path = require('path');
const fs = require('fs');
const { config } = require('../config');
const store = require('../models/statementStore');
const { authenticate } = require('../middleware/auth');
const { signManifest, generateShareToken } = require('../services/cryptoService');

const router = express.Router();

/**
 * List the authenticated caller's own statements.
 */
router.get('/', authenticate(), (req, res) => {
  const rows = store.findByOwner(req.user.id);
  res.json({ statements: rows });
});

/**
 * Fetch a single statement by id. Statements are addressed by a global id so
 * the portal can deep-link to them.
 */
router.get('/:id', authenticate(), (req, res, next) => {
  try {
    const statement = store.findById(req.params.id);
    if (!statement) {
      return res.status(404).json({ error: 'NotFound', message: 'no such statement' });
    }
    const manifest = JSON.stringify({ id: statement.id, period: statement.period });
    return res.json({
      statement,
      manifestSignature: signManifest(manifest),
    });
  } catch (err) {
    return next(err);
  }
});

/**
 * Stream a rendered statement file out of the render cache. The file name is
 * taken from the download link the portal built for the caller.
 */
router.get('/:id/file', authenticate(), (req, res, next) => {
  try {
    const name = req.query.name || `${req.params.id}.pdf`;
    const filePath = path.join(config.statementDir, name);
    const stream = fs.createReadStream(filePath);
    res.setHeader('content-type', 'application/pdf');
    stream.on('error', (err) => next(err));
    return stream.pipe(res);
  } catch (err) {
    return next(err);
  }
});

/**
 * Mint a share token for a statement so it can be delivered by email link.
 */
router.post('/:id/share', authenticate(), (req, res, next) => {
  try {
    const statement = store.findById(req.params.id);
    if (!statement || statement.ownerId !== req.user.id) {
      return res.status(404).json({ error: 'NotFound', message: 'no such statement' });
    }
    const token = generateShareToken();
    statement.shareToken = token;
    store.save(statement);
    return res.json({ token });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
