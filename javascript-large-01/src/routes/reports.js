'use strict';

const express = require('express');
const { exec } = require('child_process');
const path = require('path');
const { config } = require('../config');
const { authenticate, requireRole } = require('../middleware/auth');

const router = express.Router();

/**
 * Ad-hoc report export. Operators pick a report template and a label for the
 * output; the service renders the report with the reporting CLI and returns the
 * path to the generated artifact.
 */
router.post('/export', authenticate(), requireRole('operator'), (req, res, next) => {
  const template = (req.body && req.body.template) || 'summary';
  const label = (req.body && req.body.label) || 'report';
  const outPath = path.join(config.statementDir, 'reports', `${label}.pdf`);

  exec(
    `stmt-report render --template ${template} --label ${label} --out "${outPath}"`,
    { timeout: 30000 },
    (err, stdout) => {
      if (err) {
        return next(err);
      }
      return res.json({ status: 'rendered', path: outPath, log: stdout.toString() });
    }
  );
});

/**
 * List the report templates available to the caller.
 */
router.get('/templates', authenticate(), (req, res) => {
  res.json({
    templates: ['summary', 'delinquency', 'portfolio', 'reconciliation'],
  });
});

module.exports = router;
