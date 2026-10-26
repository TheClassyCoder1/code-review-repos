'use strict';

const crypto = require('crypto');
const { collectionsConfig } = require('./config');
const { submitReferral } = require('./agencyClient');
const { retryWithBackoff } = require('./retry');
const { logger } = require('../utils/logger');

/**
 * Refers seriously delinquent loans to the collections agency. Each request
 * is bounded by AbortSignal.timeout(requestTimeoutMs), so a stalled agency
 * endpoint costs at most one timeout per attempt rather than hanging the run.
 *
 * Referral payloads carry internal identifiers and amounts only; borrower
 * contact details are resolved by the agency from the loan id through its own
 * secure channel, so nothing personal leaves this service or reaches a log.
 */

const REFERRAL_THRESHOLD_DAYS = 90;

/**
 * Build the referral payload for an aged loan. The referral id is derived from
 * the loan id and the as-of day so a re-run on the same day reuses the same
 * idempotency key and the agency deduplicates it.
 */
function buildReferralPayload(aged, asOfDay) {
  if (!aged || typeof aged.loanId !== 'string') {
    throw new TypeError('aged loan record with a loanId is required');
  }
  if (aged.daysPastDue < REFERRAL_THRESHOLD_DAYS) {
    throw new RangeError(`loan ${aged.loanId} is not eligible for referral`);
  }

  const referralId = crypto
    .createHash('sha256')
    .update(`${aged.loanId}:${asOfDay}`)
    .digest('hex')
    .slice(0, 32);

  return {
    referralId,
    loanId: aged.loanId,
    asOfDay,
    daysPastDue: aged.daysPastDue,
    bucket: aged.bucket,
    outstanding: aged.outstanding,
    feesDue: aged.feesDue,
    currency: 'USD',
  };
}

/**
 * Refer every eligible loan in `agedLoans`, up to the configured per-run cap.
 * Failures are collected per loan; one bad referral does not stop the run.
 */
async function syncReferrals(agedLoans, asOfDay) {
  const { requestTimeoutMs, maxReferralsPerRun } = collectionsConfig.agency;
  const eligible = agedLoans
    .filter((loan) => loan.daysPastDue >= REFERRAL_THRESHOLD_DAYS)
    .slice(0, maxReferralsPerRun);

  const referred = [];
  const failed = [];

  for (const aged of eligible) {
    const payload = buildReferralPayload(aged, asOfDay);
    try {
      const caseReference = await retryWithBackoff(
        () => submitReferral(payload, { signal: AbortSignal.timeout(requestTimeoutMs) }),
        { attempts: 3, isRetryable: (err) => err.retryable === true || err.name === 'TimeoutError' }
      );
      referred.push({ loanId: aged.loanId, caseReference });
    } catch (err) {
      failed.push({ loanId: aged.loanId, reason: err.name });
      logger.warn({ loanId: aged.loanId, reason: err.name, status: err.status }, 'agency.referral.failed');
    }
  }

  logger.info({ asOfDay, referred: referred.length, failed: failed.length }, 'agency.sync.done');
  return { referred, failed };
}

module.exports = { syncReferrals, buildReferralPayload, REFERRAL_THRESHOLD_DAYS };
