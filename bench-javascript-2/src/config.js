'use strict';

function int(name, fallback) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') return fallback;
  const value = Number.parseInt(raw, 10);
  if (!Number.isInteger(value)) throw new Error(`${name} must be an integer`);
  return value;
}

function required(name) {
  const value = process.env[name];
  if (!value) throw new Error(`${name} is required`);
  return value;
}

module.exports = {
  port: int('PORT', 8080),
  databaseUrl: required('DATABASE_URL'),
  redisUrl: process.env.REDIS_URL || 'redis://localhost:6379',
  jwtSecret: required('JWT_SECRET'),
  webhookSecret: required('COURIER_WEBHOOK_SECRET'),
  bankApiUrl: required('BANK_API_URL'),
  manifestArchiveDir: process.env.MANIFEST_ARCHIVE_DIR || '/var/lib/dispatch/manifests',
  routing: {
    baseUrl: process.env.ROUTING_API_URL || 'http://routing.internal:7000',
    timeoutMs: int('ROUTING_TIMEOUT_MS', 3000),
  },
  routeCacheTtlSec: int('ROUTE_CACHE_TTL_SEC', 60),
  pricing: {
    baseFeeCents: int('BASE_FEE_CENTS', 250),
    perKmCents: int('PER_KM_CENTS', 45),
  },
  maxPageSize: 100,
};
