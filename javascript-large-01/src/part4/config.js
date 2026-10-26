'use strict';

/**
 * Configuration for the collections and payment-plan features. Values come
 * from the environment with local-development fallbacks, mirroring the
 * conventions in src/config. Nothing here is a secret: the collections agency
 * credential is read lazily by the agency client so it is never captured in a
 * long-lived object that could end up in a log line.
 */

function env(name, fallback) {
  const value = process.env[name];
  if (value === undefined || value === '') {
    return fallback;
  }
  return value;
}

function positiveInt(name, fallback) {
  const parsed = Number.parseInt(env(name, String(fallback)), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

const collectionsConfig = {
  // Where nightly delinquency exports are spooled before upload.
  spoolDir: env('COLLECTIONS_SPOOL_DIR', '/var/lib/statements/collections'),

  // Where payment receipts are staged before they are handed to the archive.
  receiptDir: env('RECEIPT_STAGING_DIR', '/var/lib/statements/receipts'),

  agency: {
    baseUrl: env('COLLECTIONS_AGENCY_URL', 'http://127.0.0.1:8230'),
    requestTimeoutMs: positiveInt('COLLECTIONS_AGENCY_TIMEOUT_MS', 5000),
    maxReferralsPerRun: positiveInt('COLLECTIONS_MAX_REFERRALS', 200),
  },

  plans: {
    minInstallments: 2,
    maxInstallments: positiveInt('PAYMENT_PLAN_MAX_INSTALLMENTS', 24),
    // Annual rate applied to plan balances, in basis points.
    defaultRateBps: positiveInt('PAYMENT_PLAN_RATE_BPS', 450),
  },

  delinquency: {
    gracePeriodDays: positiveInt('DELINQUENCY_GRACE_DAYS', 5),
    lateFeeFlat: Number(env('DELINQUENCY_LATE_FEE_FLAT', '25')),
    lateFeePct: Number(env('DELINQUENCY_LATE_FEE_PCT', '0.05')),
    lateFeeCap: Number(env('DELINQUENCY_LATE_FEE_CAP', '75')),
    // Progress heartbeat for long batch runs.
    heartbeatMs: positiveInt('DELINQUENCY_HEARTBEAT_MS', 15000),
  },
};

module.exports = { collectionsConfig };
