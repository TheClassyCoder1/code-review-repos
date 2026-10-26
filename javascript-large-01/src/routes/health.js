'use strict';

const express = require('express');
const { config } = require('../config');

const router = express.Router();

/**
 * Liveness and readiness probes. Mounted unauthenticated so the platform
 * supervisor can reach them.
 */
router.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'statement-export-service', env: config.env });
});

router.get('/ready', (req, res) => {
  res.json({
    status: 'ready',
    archive: Boolean(config.archive.baseUrl),
    uptimeSeconds: Math.round(process.uptime()),
  });
});

module.exports = router;
