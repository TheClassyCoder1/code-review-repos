'use strict';

const express = require('express');
const { authenticate } = require('../../middleware/auth');
const csvExporter = require('../services/csvExporter');
const exportQuotaService = require('../services/exportQuotaService');
const { part3Config } = require('../config/part3Config');
const { assertSafeId, ValidationError } = require('../../utils/validate');
const { signManifest } = require('../../services/cryptoService');

const router = express.Router();

/**
 * Validate the list of statement ids for a bulk export: non-empty, unique,
 * well-formed, and no more than maxIdsPerExport entries.
 */
function parseStatementIds(raw) {
  if (!Array.isArray(raw) || raw.length === 0) {
    throw new ValidationError('statementIds must be a non-empty array');
  }
  const { maxIdsPerExport } = part3Config.exports;
  if (raw.length > maxIdsPerExport) {
    throw new ValidationError(`at most ${maxIdsPerExport} statements per export`);
  }
  return [...new Set(raw.map((id) => assertSafeId(String(id))))];
}

/**
 * Bulk CSV export of the caller's statement summaries.
 */
router.post('/statements.csv', authenticate(), async (req, res, next) => {
  try {
    const ids = parseStatementIds(req.body.statementIds);
    const csv = await exportQuotaService.runWithinQuota(req.user.id, async () => {
      const rows = csvExporter.resolveOwnedRows(req.user.id, ids);
      return csvExporter.toCsv(rows);
    });
    res.setHeader('content-type', 'text/csv; charset=utf-8');
    res.setHeader('x-export-signature', signManifest(csv));
    return res.send(csv);
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
