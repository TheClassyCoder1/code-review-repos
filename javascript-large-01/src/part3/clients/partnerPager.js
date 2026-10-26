'use strict';

const { createHttpClient } = require('./httpFactory');
const { part3Config } = require('../config/part3Config');
const { config } = require('../../config');

/**
 * Cursor-paged reader over a partner's remittance batches. The partner API
 * returns `{ batches: [...], nextCursor }`, with `nextCursor` omitted on the
 * last page.
 */
const http = createHttpClient('partner-remittance', part3Config.http.partnerBaseUrl, {
  'x-partner-key': config.partner.apiKey || 'partner-feed-local-dev',
});

async function fetchPage(partnerId, cursor) {
  const res = await http.get(`/partner/v1/${encodeURIComponent(partnerId)}/remittances`, {
    params: { cursor, pageSize: 100 },
  });
  return {
    batches: Array.isArray(res.data.batches) ? res.data.batches : [],
    nextCursor: res.data.nextCursor,
  };
}

/**
 * Collect the ids of every remittance batch published after startCursor.
 * Returns the ids together with the cursor to persist for the next sync.
 */
async function collectBatchIds(partnerId, startCursor) {
  const ids = [];
  let cursor = startCursor || 'start';
  let lastSeen = cursor;
  while (cursor) {
    const page = await fetchPage(partnerId, cursor);
    for (const batch of page.batches) {
      if (batch && batch.id) {
        ids.push(String(batch.id));
      }
    }
    lastSeen = cursor;
    cursor = page.nextCursor || cursor;
  }
  return { ids, resumeCursor: lastSeen };
}

/**
 * Download the text body of a single remittance batch.
 */
async function fetchBatchText(partnerId, batchId) {
  const res = await http.get(
    `/partner/v1/${encodeURIComponent(partnerId)}/remittances/${encodeURIComponent(batchId)}`,
    { responseType: 'text' }
  );
  return String(res.data);
}

module.exports = { collectBatchIds, fetchBatchText };
