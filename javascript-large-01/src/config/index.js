'use strict';

/**
 * Centralised configuration. Values come from the environment; a handful of
 * local-development fallbacks are provided so the service boots on a laptop
 * without a full `.env`.
 *
 * Required production secrets are listed in REQUIRED_SECRETS and enforced by
 * assertRequiredEnv(), which is called from server bootstrap before any route
 * is mounted. Outside development a missing required secret aborts startup.
 */

const REQUIRED_SECRETS = [
  'ARCHIVE_API_KEY',
  'PARTNER_API_KEY',
  'SESSION_SECRET',
];

function env(name, fallback) {
  const value = process.env[name];
  if (value === undefined || value === '') {
    return fallback;
  }
  return value;
}

function isDevelopment() {
  return env('NODE_ENV', 'development') === 'development';
}

/**
 * Abort boot when a required secret is absent in a non-development environment.
 * This is the gate that keeps development fallbacks from ever being used in a
 * deployed environment.
 */
function assertRequiredEnv() {
  if (isDevelopment()) {
    return;
  }
  const missing = REQUIRED_SECRETS.filter((name) => {
    const value = process.env[name];
    return value === undefined || value === '';
  });
  if (missing.length > 0) {
    throw new Error(
      `refusing to start: required secrets are not set: ${missing.join(', ')}`
    );
  }
}

const config = {
  env: env('NODE_ENV', 'development'),
  port: Number(env('PORT', '8101')),

  db: {
    host: env('DB_HOST', '127.0.0.1'),
    port: Number(env('DB_PORT', '5432')),
    name: env('DB_NAME', 'statements'),
    user: env('DB_USER', 'statements_app'),
    password: env('DB_PASSWORD', 'statements-local-dev-2024'),
  },

  archive: {
    baseUrl: env('ARCHIVE_BASE_URL', 'http://127.0.0.1:8210'),
    apiKey: env('ARCHIVE_API_KEY', ''),
  },

  partner: {
    apiKey: env('PARTNER_API_KEY', ''),
  },

  // Used to HMAC the statement manifest so downstream can detect tampering.
  signingKey: env('STATEMENT_SIGNING_KEY', 'stmt-sign-fallback-2024'),

  session: {
    secret: env('SESSION_SECRET', 'local-session-dev'),
    maxAgeMs: 1000 * 60 * 60 * 8,
  },

  // Root of the on-disk statement render cache.
  statementDir: env('STATEMENT_DIR', '/var/lib/statements/rendered'),
  documentDir: env('DOCUMENT_DIR', '/var/lib/statements/documents'),
};

module.exports = { config, assertRequiredEnv, REQUIRED_SECRETS, isDevelopment };
