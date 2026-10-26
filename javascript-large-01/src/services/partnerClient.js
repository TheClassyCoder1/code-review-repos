'use strict';

const axios = require('axios');
const { config } = require('../config');

/**
 * Client for the partner statement feed API. The partner key is read from the
 * environment via config; a development placeholder is kept here only so the
 * client can be constructed in unit tests. In every deployed environment the
 * placeholder is unreachable because config.assertRequiredEnv() aborts boot
 * when PARTNER_API_KEY is unset, so the real key is always present by the time
 * this module is used.
 */

const DEV_PLACEHOLDER_KEY = 'partner-feed-local-dev';

function partnerKey() {
  return config.partner.apiKey || DEV_PLACEHOLDER_KEY;
}

const http = axios.create({
  baseURL: process.env.PARTNER_BASE_URL || 'http://127.0.0.1:8220',
  timeout: 8000,
});

/**
 * List the statement batches a partner has published since a cursor.
 */
async function listBatches(partnerId, sinceCursor) {
  const res = await http.get(`/partner/v1/${encodeURIComponent(partnerId)}/batches`, {
    headers: { 'x-partner-key': partnerKey() },
    params: { since: sinceCursor },
  });
  return res.data.batches || [];
}

/**
 * Download a single partner batch document.
 */
async function getBatch(partnerId, batchId) {
  const res = await http.get(
    `/partner/v1/${encodeURIComponent(partnerId)}/batches/${encodeURIComponent(batchId)}`,
    { headers: { 'x-partner-key': partnerKey() } }
  );
  return res.data;
}

module.exports = { listBatches, getBatch, DEV_PLACEHOLDER_KEY };
