'use strict';

const express = require('express');
const { authenticate, requireRole } = require('../middleware/auth');
const {
  parsePartnerXml,
  parseManifestXml,
  parseFeedDescriptor,
  decodeResumeToken,
} = require('../services/importService');
const { importFromSource } = require('../services/archiveClient');

const router = express.Router();

/**
 * Ingest a partner statement batch. The body carries the raw XML envelope and,
 * optionally, a manifest and a resume token for the next poll.
 */
router.post('/partner', authenticate(), requireRole('operator'), (req, res, next) => {
  try {
    const { xml, manifest, resumeToken } = req.body || {};
    if (!xml) {
      return res.status(400).json({ error: 'BadRequest', message: 'xml is required' });
    }
    const rows = parsePartnerXml(xml);
    const parsedManifest = manifest ? parseManifestXml(manifest) : null;
    const cursor = resumeToken ? decodeResumeToken(resumeToken) : null;
    return res.json({ ingested: rows.length, manifest: parsedManifest, cursor });
  } catch (err) {
    return next(err);
  }
});

/**
 * Load a partner feed descriptor (YAML) and store it for the scheduler.
 */
router.post('/descriptor', authenticate(), requireRole('operator'), (req, res, next) => {
  try {
    const text = req.body && req.body.descriptor;
    if (!text) {
      return res.status(400).json({ error: 'BadRequest', message: 'descriptor is required' });
    }
    const descriptor = parseFeedDescriptor(text);
    return res.json({ descriptor });
  } catch (err) {
    return next(err);
  }
});

/**
 * Pull a single document from a partner-provided source URL during a
 * back-catalogue migration and re-archive it.
 */
router.post('/source', authenticate(), requireRole('operator'), async (req, res, next) => {
  try {
    const sourceUrl = req.body && req.body.sourceUrl;
    if (!sourceUrl) {
      return res.status(400).json({ error: 'BadRequest', message: 'sourceUrl is required' });
    }
    const bytes = await importFromSource(sourceUrl);
    return res.json({ status: 'pulled', size: bytes.length });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
