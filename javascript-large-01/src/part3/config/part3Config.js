'use strict';

/**
 * Configuration for the repayments and collections features. Every numeric knob
 * is parsed once at load time and clamped into a sane range so downstream code
 * can rely on finite integers without re-validating.
 */

function intFromEnv(name, fallback, min, max) {
  const raw = Number.parseInt(process.env[name] || '', 10);
  if (!Number.isFinite(raw)) {
    return fallback;
  }
  return Math.min(max, Math.max(min, raw));
}

function strFromEnv(name, fallback) {
  const value = process.env[name];
  return value === undefined || value === '' ? fallback : value;
}

const PRODUCT_CODES = Object.freeze([
  'PERSONAL_12',
  'PERSONAL_36',
  'PERSONAL_60',
  'AUTO_48',
  'AUTO_72',
  'SME_TERM_24',
]);

const part3Config = Object.freeze({
  http: Object.freeze({
    // Applied to every client built through clients/httpFactory.
    defaultTimeoutMs: intFromEnv('P3_HTTP_TIMEOUT_MS', 5000, 250, 30000),
    bureauBaseUrl: strFromEnv('P3_BUREAU_BASE_URL', 'http://127.0.0.1:8230'),
    notifyBaseUrl: strFromEnv('P3_NOTIFY_BASE_URL', 'http://127.0.0.1:8240'),
    collectionsBaseUrl: strFromEnv('P3_COLLECTIONS_BASE_URL', 'http://127.0.0.1:8250'),
    partnerBaseUrl: strFromEnv('PARTNER_BASE_URL', 'http://127.0.0.1:8220'),
  }),

  collections: Object.freeze({
    apiKey: strFromEnv('P3_COLLECTIONS_API_KEY', 'collections-local-dev-2024'),
    minDaysPastDue: intFromEnv('P3_COLLECTIONS_MIN_DPD', 90, 30, 365),
  }),

  retry: Object.freeze({
    maxAttempts: intFromEnv('P3_RETRY_MAX_ATTEMPTS', 4, 1, 8),
    baseDelayMs: intFromEnv('P3_RETRY_BASE_DELAY_MS', 200, 10, 5000),
    maxDelayMs: intFromEnv('P3_RETRY_MAX_DELAY_MS', 5000, 100, 60000),
  }),

  exports: Object.freeze({
    dailyLimit: intFromEnv('P3_EXPORT_DAILY_LIMIT', 5, 1, 50),
    maxIdsPerExport: intFromEnv('P3_EXPORT_MAX_IDS', 200, 1, 1000),
  }),

  reminders: Object.freeze({
    sendTimeoutMs: intFromEnv('P3_REMINDER_SEND_TIMEOUT_MS', 4000, 250, 20000),
    initialBackoffMs: intFromEnv('P3_REMINDER_BACKOFF_MS', 500, 50, 10000),
    maxBackoffMs: intFromEnv('P3_REMINDER_MAX_BACKOFF_MS', 30000, 1000, 300000),
  }),

  remittance: Object.freeze({
    maxBodyBytes: intFromEnv('P3_REMITTANCE_MAX_BYTES', 512 * 1024, 1024, 8 * 1024 * 1024),
  }),

  schedules: Object.freeze({
    maxPrincipal: 1000000,
    maxTermMonths: 480,
  }),

  products: PRODUCT_CODES,
});

module.exports = { part3Config, PRODUCT_CODES, intFromEnv };
