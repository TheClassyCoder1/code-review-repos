'use strict';

const { collectionsConfig } = require('./config');

/**
 * Client for the external collections agency referral API. Loans that age
 * past 90 days are referred to the agency; the agency acknowledges each
 * referral with its own case reference.
 *
 * Deadlines are owned by the caller: every request takes an AbortSignal and
 * agencySyncJob supplies one bounded by the configured request timeout.
 */

const DEV_PLACEHOLDER_TOKEN = 'collections-agency-local-dev';

class AgencyError extends Error {
  constructor(message, status, retryable) {
    super(message);
    this.name = 'AgencyError';
    this.status = status;
    this.retryable = retryable;
  }
}

function agencyToken() {
  return process.env.COLLECTIONS_AGENCY_TOKEN || DEV_PLACEHOLDER_TOKEN;
}

/**
 * Submit one referral. Returns the agency case reference.
 */
async function submitReferral(referral, { signal } = {}) {
  const res = await fetch(`${collectionsConfig.agency.baseUrl}/referrals/v2`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      authorization: `Bearer ${agencyToken()}`,
      'idempotency-key': referral.referralId,
    },
    body: JSON.stringify(referral),
    signal,
  });

  if (!res.ok) {
    const retryable = res.status === 429 || res.status >= 500;
    throw new AgencyError(`agency rejected referral with ${res.status}`, res.status, retryable);
  }
  const body = await res.json();
  return body.caseReference;
}

module.exports = { submitReferral, AgencyError };
