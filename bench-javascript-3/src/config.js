'use strict';

function required(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable ${name}`);
  }
  return value;
}

function int(name, fallback) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') return fallback;
  const parsed = Number.parseInt(raw, 10);
  if (!Number.isInteger(parsed)) {
    throw new Error(`Environment variable ${name} must be an integer`);
  }
  return parsed;
}

module.exports = {
  port: int('PORT', 8080),
  host: process.env.HOST || '0.0.0.0',
  mongoUri: required('MONGO_URI'),
  jwtSecret: required('JWT_SECRET'),
  sessionTtlSec: int('SESSION_TTL_SEC', 3600),
  cookiePassword: required('COOKIE_PASSWORD'),
  uploadDir: process.env.UPLOAD_DIR || '/var/lib/forum/uploads',
  maxUploadBytes: int('MAX_UPLOAD_BYTES', 10 * 1024 * 1024),
  moderationWebhookUrl: required('MODERATION_WEBHOOK_URL'),
  outboundTimeoutMs: int('OUTBOUND_TIMEOUT_MS', 4000),
  sso: {
    tokenUrl: process.env.SSO_TOKEN_URL || 'https://sso.example.org/oauth/token',
    clientId: process.env.SSO_CLIENT_ID || 'forum',
    clientSecret: process.env.SSO_CLIENT_SECRET || '',
  },
  publicBaseUrl: process.env.PUBLIC_BASE_URL || 'https://forum.example.org',
};
