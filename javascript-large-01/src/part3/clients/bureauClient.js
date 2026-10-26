'use strict';

const { createHttpClient } = require('./httpFactory');
const { part3Config } = require('../config/part3Config');

/**
 * Credit bureau client used before a delinquent loan is referred to an outside
 * agency. The bureau is addressed by our opaque subject reference, never by a
 * national id.
 */
const http = createHttpClient('bureau', part3Config.http.bureauBaseUrl);

/**
 * Pull the latest tradeline summary for a subject.
 */
async function fetchTradelineSummary(subjectRef) {
  const res = await http.get(`/bureau/v1/reports/${encodeURIComponent(subjectRef)}`);
  const body = res.data || {};
  return {
    subjectRef,
    openTradelines: Number(body.openTradelines || 0),
    worstDelinquencyDays: Number(body.worstDelinquencyDays || 0),
    pulledAt: body.pulledAt || new Date().toISOString(),
  };
}

/**
 * Notify the bureau that a tradeline has been placed with collections.
 */
async function reportPlacement(subjectRef, loanId, placedOn) {
  await http.post(`/bureau/v1/reports/${encodeURIComponent(subjectRef)}/events`, {
    type: 'COLLECTIONS_PLACEMENT',
    accountRef: loanId,
    effectiveDate: placedOn,
  });
}

module.exports = { fetchTradelineSummary, reportPlacement };
