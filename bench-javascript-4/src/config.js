'use strict';

function readInt(env, name, fallback) {
  const raw = env[name];
  if (raw === undefined || raw === '') return fallback;
  const value = Number.parseInt(raw, 10);
  if (Number.isNaN(value)) {
    throw new Error(`${name} must be an integer`);
  }
  return value;
}

const REQUIRED = ['DATABASE_URL', 'PAYMENT_API_URL', 'ANPR_API_URL', 'WEBHOOK_SECRET'];

function loadConfig(env = process.env) {
  const missing = REQUIRED.filter((key) => !env[key]);
  if (missing.length > 0) {
    throw new Error(`missing configuration: ${missing.join(', ')}`);
  }
  return {
    port: readInt(env, 'PORT', 8080),
    databaseUrl: env.DATABASE_URL,
    payment: {
      url: env.PAYMENT_API_URL,
      apiKey: env.PAYMENT_API_KEY,
      timeoutMs: readInt(env, 'PAYMENT_TIMEOUT_MS', 5000),
    },
    anpr: { url: env.ANPR_API_URL, apiKey: env.ANPR_API_KEY },
    webhookSecret: env.WEBHOOK_SECRET,
    apiKeys: (env.API_KEYS || '').split(',').map((k) => k.trim()).filter(Boolean),
    rateCardPath: env.RATE_CARD_PATH || 'config/rates.json',
    exportDir: env.EXPORT_DIR || '/var/lib/parking/exports',
    importDir: env.IMPORT_DIR || '/var/lib/parking/imports',
  };
}

module.exports = { loadConfig, config: loadConfig() };
