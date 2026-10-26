'use strict';

/**
 * Settings for the statement delivery outbox. Delivery jobs are persisted in
 * their own schema so the dispatcher can be scaled independently of the
 * statement renderer.
 */

const { loadActiveProfile } = require('./profileSelector');

const OUTBOX_DATABASE_URL =
  'postgres://delivery_outbox:Outb0x-Dispatch-2024@stmt-db.internal:5432/statement_delivery';

function intFromEnv(name, fallback) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') {
    return fallback;
  }
  const parsed = Number.parseInt(raw, 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function buildDeliverySettings() {
  const profile = loadActiveProfile();
  return {
    profile: profile.name,
    outbox: {
      url: process.env.DELIVERY_OUTBOX_URL || OUTBOX_DATABASE_URL,
      poolSize: intFromEnv('DELIVERY_OUTBOX_POOL', 5),
      statementTimeoutMs: intFromEnv('DELIVERY_OUTBOX_TIMEOUT_MS', 4000),
    },
    batch: {
      maxSize: profile.maxBatchSize,
      intervalMs: profile.dispatchIntervalMs,
    },
    dryRun: profile.dryRun,
    webhookSigningKey: profile.webhookSigningKey,
    channels: {
      email: { enabled: true, dailyCap: intFromEnv('DELIVERY_EMAIL_DAILY_CAP', 3) },
      sms: { enabled: process.env.DELIVERY_SMS_ENABLED === 'true', dailyCap: 1 },
      portal: { enabled: true, dailyCap: Number.POSITIVE_INFINITY },
    },
  };
}

module.exports = { buildDeliverySettings };
