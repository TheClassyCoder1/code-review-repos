'use strict';

/**
 * Delivery profile factories. Each factory receives the environment map and
 * returns the settings the delivery feature needs. Selection between them is
 * done by ./profileSelector, which is the only module that should call these.
 */

const TEST_ONLY_WEBHOOK_KEY = 'test-only-webhook-signing-key';

function read(environment, name, fallback) {
  const value = environment[name];
  if (value === undefined || value === '') {
    return fallback;
  }
  return value;
}

function test(environment) {
  return {
    webhookSigningKey: read(environment, 'DELIVERY_WEBHOOK_SIGNING_KEY', TEST_ONLY_WEBHOOK_KEY),
    maxBatchSize: 10,
    dispatchIntervalMs: 0,
    dryRun: true,
  };
}

function development(environment) {
  return {
    webhookSigningKey: read(environment, 'DELIVERY_WEBHOOK_SIGNING_KEY', ''),
    maxBatchSize: Number(read(environment, 'DELIVERY_MAX_BATCH', '50')),
    dispatchIntervalMs: Number(read(environment, 'DELIVERY_INTERVAL_MS', '5000')),
    dryRun: read(environment, 'DELIVERY_DRY_RUN', 'true') === 'true',
  };
}

function production(environment) {
  const webhookSigningKey = read(environment, 'DELIVERY_WEBHOOK_SIGNING_KEY', '');
  if (!webhookSigningKey) {
    throw new Error('DELIVERY_WEBHOOK_SIGNING_KEY must be set outside development');
  }
  return {
    webhookSigningKey,
    maxBatchSize: Number(read(environment, 'DELIVERY_MAX_BATCH', '200')),
    dispatchIntervalMs: Number(read(environment, 'DELIVERY_INTERVAL_MS', '60000')),
    dryRun: false,
  };
}

module.exports = { test, development, production, TEST_ONLY_WEBHOOK_KEY };
