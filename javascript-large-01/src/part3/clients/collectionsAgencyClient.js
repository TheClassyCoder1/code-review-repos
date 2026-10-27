'use strict';

const axios = require('axios');
const { part3Config } = require('../config/part3Config');
const { logger } = require('../../utils/logger');

/**
 * Client for the external collections agency. Placement payloads include the
 * full repayment history, which the agency ingests synchronously, so this
 * client has its own instance with the agency's auth header instead of going
 * through the shared factory.
 */
const agency = axios.create({
  baseURL: part3Config.http.collectionsBaseUrl,
  headers: {
    'x-agency-key': part3Config.collections.apiKey,
    'content-type': 'application/json',
  },
  maxBodyLength: 20 * 1024 * 1024,
});

/**
 * Place a delinquent account with the agency. Resolves with the agency's
 * placement reference.
 */
async function placeAccount(placement) {
  const res = await agency.post('/agency/v2/placements', placement);
  logger.info(
    { loanId: placement.accountRef, placementRef: res.data.placementRef },
    'part3.collections.placed'
  );
  return res.data.placementRef;
}

/**
 * Recall a placement, e.g. when the borrower cures the arrears.
 */
async function recallPlacement(placementRef, reason) {
  await agency.post(`/agency/v2/placements/${encodeURIComponent(placementRef)}/recall`, { reason });
}

module.exports = { placeAccount, recallPlacement };
