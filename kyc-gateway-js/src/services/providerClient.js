'use strict';

const axios = require('axios');
const config = require('../config');

const http = axios.create({ timeout: config.providerTimeoutMs, maxRedirects: 0 });

function normalizeReport(data) {
  return {
    provider: data.provider || 'unknown',
    document: data.document_check?.result === 'clear' ? 'clear' : 'consider',
    watchlist: data.watchlist_check?.result === 'clear' ? 'clear' : 'consider',
    receivedAt: new Date().toISOString(),
  };
}

function providerName(reportUrl) {
  const host = new URL(reportUrl).hostname;
  if (host.includes('onfido')) return 'onfido';
  if (host.includes('sumsub')) return 'sumsub';
  return host;
}

async function fetchProviderReport(reportUrl) {
  const response = await http.get(reportUrl, { headers: { Accept: 'application/json' } });
  return normalizeReport({ provider: providerName(reportUrl), ...response.data });
}

module.exports = { fetchProviderReport };
