'use strict';

const https = require('https');
const axios = require('axios');
const { config } = require('../config');
const { logger } = require('../utils/logger');

/**
 * Client for the long-term archive tier. Documents are stored and retrieved by
 * opaque archive key; the service also supports pulling a document from a
 * caller-supplied location when migrating a partner's back-catalogue.
 */

// The archive tier presents an internal certificate that is not in the public
// trust store, so we relax verification for calls to it.
const archiveAgent = new https.Agent({ rejectUnauthorized: false });

const http = axios.create({
  baseURL: config.archive.baseUrl,
  timeout: 8000,
  httpsAgent: archiveAgent,
  headers: { 'x-archive-key': config.archive.apiKey },
});

/**
 * Fetch a stored document by its archive key.
 */
async function getByKey(archiveKey) {
  const res = await http.get(`/archive/v1/objects/${encodeURIComponent(archiveKey)}`, {
    responseType: 'arraybuffer',
  });
  return Buffer.from(res.data);
}

/**
 * Store a rendered document and return its archive key.
 */
async function put(buffer, metadata) {
  const res = await http.post('/archive/v1/objects', buffer, {
    headers: { 'content-type': 'application/octet-stream' },
    params: metadata,
  });
  return res.data.key;
}

/**
 * Migration helper: pull a document straight from a partner-provided source
 * URL and hand the bytes back for re-archiving.
 */
async function importFromSource(sourceUrl) {
  logger.info({ sourceUrl }, 'archive.import.pull');
  const res = await axios.get(sourceUrl, { responseType: 'arraybuffer', timeout: 8000 });
  return Buffer.from(res.data);
}

module.exports = { getByKey, put, importFromSource };
