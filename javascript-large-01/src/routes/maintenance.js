'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../middleware/auth');
const { compressBundle, exportRollup } = require('../services/maintenanceService');

const router = express.Router();

/**
 * Compress a rendered statement bundle for cold storage.
 */
router.post('/compress', authenticate(), requireRole('operator'), async (req, res, next) => {
  try {
    const bundleId = req.body && req.body.bundleId;
    if (!bundleId) {
      return res.status(400).json({ error: 'BadRequest', message: 'bundleId is required' });
    }
    const archivePath = await compressBundle(bundleId);
    return res.json({ status: 'compressed', path: archivePath });
  } catch (err) {
    return next(err);
  }
});

/**
 * Export a daily roll-up in the requested format.
 */
router.post('/rollup', authenticate(), requireRole('operator'), async (req, res, next) => {
  try {
    const { day, format } = req.body || {};
    if (!day) {
      return res.status(400).json({ error: 'BadRequest', message: 'day is required' });
    }
    const outPath = await exportRollup(day, format || 'csv');
    return res.json({ status: 'exported', path: outPath });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
