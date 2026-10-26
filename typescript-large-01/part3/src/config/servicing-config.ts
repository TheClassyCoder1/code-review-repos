/**
 * Runtime configuration for the servicing module. Every value can be overridden
 * through the environment; defaults are tuned for a single-node dev deployment.
 */
export interface ServicingConfig {
  bureauBaseUrl: string;
  bureauTimeoutMs: number;
  gatewayBaseUrl: string;
  gatewayApiKey: string;
  ledgerBaseUrl: string;
  ledgerTimeoutMs: number;
  quoteCacheSize: number;
  delinquentPageSize: number;
  maxDelinquentPages: number;
  maxUploadBytes: number;
  settlementMaxPolls: number;
  settlementPollIntervalMs: number;
  lockDir: string;
  exportDir: string;
  dbPoolSize: number;
}

function intEnv(env: NodeJS.ProcessEnv, name: string, fallback: number, min: number, max: number): number {
  const raw = env[name]?.trim();
  if (!raw) {
    return fallback;
  }
  const parsed = Number.parseInt(raw, 10);
  if (!Number.isFinite(parsed)) {
    throw new Error(`${name} must be an integer`);
  }
  return Math.min(max, Math.max(min, parsed));
}

function strEnv(env: NodeJS.ProcessEnv, name: string, fallback: string): string {
  return env[name]?.trim() || fallback;
}

export function loadServicingConfig(env: NodeJS.ProcessEnv = process.env): ServicingConfig {
  return {
    bureauBaseUrl: strEnv(env, 'BUREAU_BASE_URL', 'http://localhost:8301'),
    bureauTimeoutMs: intEnv(env, 'BUREAU_TIMEOUT_MS', 4000, 250, 15000),
    gatewayBaseUrl: strEnv(env, 'PAYOUT_GATEWAY_URL', 'http://localhost:8302'),
    gatewayApiKey: strEnv(env, 'PAYOUT_GATEWAY_KEY', 'svc-payouts-dev-key-2024'),
    ledgerBaseUrl: strEnv(env, 'LEDGER_BASE_URL', 'http://localhost:8303'),
    ledgerTimeoutMs: intEnv(env, 'LEDGER_TIMEOUT_MS', 5000, 250, 20000),
    quoteCacheSize: intEnv(env, 'QUOTE_CACHE_SIZE', 2000, 16, 50000),
    delinquentPageSize: intEnv(env, 'DELINQUENT_PAGE_SIZE', 200, 10, 1000),
    maxDelinquentPages: intEnv(env, 'DELINQUENT_MAX_PAGES', 50, 1, 500),
    maxUploadBytes: intEnv(env, 'MAX_UPLOAD_BYTES', 5 * 1024 * 1024, 1024, 25 * 1024 * 1024),
    settlementMaxPolls: intEnv(env, 'SETTLEMENT_MAX_POLLS', 12, 1, 200),
    settlementPollIntervalMs: intEnv(env, 'SETTLEMENT_POLL_INTERVAL_MS', 30000, 1000, 600000),
    lockDir: strEnv(env, 'SERVICING_LOCK_DIR', '/var/run/servicing/locks'),
    exportDir: strEnv(env, 'STATEMENT_EXPORT_DIR', '/var/lib/servicing/statements'),
    dbPoolSize: intEnv(env, 'SERVICING_DB_POOL', 8, 1, 64),
  };
}
