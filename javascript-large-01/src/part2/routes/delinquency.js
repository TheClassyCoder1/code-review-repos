'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../../middleware/auth');
const repo = require('../models/delinquencyRepo');
const { exportBucket } = require('../services/delinquencyExporter');

const router = express.Router();

/**
 * Public sort keys mapped to the only column expressions the query may use.
 * The request value is a lookup key, never interpolated itself.
 */
const SORT_COLUMNS = Object.freeze({
  dpd: 'days_past_due',
  balance: 'balance_cents',
  lastPayment: 'last_payment_at',
  bucket: 'bucket',
});

function resolveSort(query) {
  const key = Object.prototype.hasOwnProperty.call(SORT_COLUMNS, query.sort) ? query.sort : 'dpd';
  const direction = String(query.dir || '').toLowerCase() === 'asc' ? 'ASC' : 'DESC';
  return { sortColumn: SORT_COLUMNS[key], direction };
}

function boundedInt(value, fallback, min, max) {
  const n = Number.parseInt(value, 10);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(max, Math.max(min, n));
}

router.get('/', authenticate(), requireRole('servicing:read'), async (req, res, next) => {
  try {
    const { sortColumn, direction } = resolveSort(req.query);
    const minDays = boundedInt(req.query.minDays, 30, 1, 3650);
    const limit = boundedInt(req.query.limit, 50, 1, 500);
    const offset = boundedInt(req.query.offset, 0, 0, 1000000);
    const rows = await repo.listDelinquent({ minDays, sortColumn, direction, limit, offset });
    return res.json({ accounts: rows, limit, offset });
  } catch (err) {
    return next(err);
  }
});

router.post('/export', authenticate(), requireRole('servicing:read'), async (req, res, next) => {
  try {
    const minDays = boundedInt(req.body && req.body.minDays, 90, 1, 3650);
    const result = await exportBucket(minDays);
    return res.status(201).json(result);
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
