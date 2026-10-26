/**
 * Runtime settings for the disbursement and servicing workers.
 *
 * Every value can be overridden from the environment; the defaults are the
 * values used on shared staging. Secrets are never defaulted here.
 */
export interface GatewaySettings {
  baseUrl: string;
  /** Hard deadline for a single gateway round-trip, connect + body. */
  requestTimeoutMs: number;
  maxAttempts: number;
  apiKeyEnv: string;
}

export interface ExportSettings {
  outputDir: string;
  rowsPerFlush: number;
}

export interface LeaseSettings {
  heartbeatIntervalMs: number;
  leaseTtlMs: number;
}

export interface ConsumerSettings {
  topic: string;
  groupId: string;
  maxBufferedMessages: number;
}

export interface Part4Settings {
  gateway: GatewaySettings;
  export: ExportSettings;
  lease: LeaseSettings;
  consumer: ConsumerSettings;
}

const DEFAULT_GATEWAY_TIMEOUT_MS = 8_000;
const MAX_GATEWAY_TIMEOUT_MS = 30_000;

function readInt(env: NodeJS.ProcessEnv, key: string, fallback: number, min: number, max: number): number {
  const raw = env[key];
  if (raw === undefined || raw.trim() === '') {
    return fallback;
  }
  const parsed = Number.parseInt(raw, 10);
  if (!Number.isFinite(parsed)) {
    throw new Error(`setting ${key} must be an integer, got "${raw}"`);
  }
  return Math.min(max, Math.max(min, parsed));
}

function readString(env: NodeJS.ProcessEnv, key: string, fallback: string): string {
  const raw = env[key];
  return raw === undefined || raw.trim() === '' ? fallback : raw.trim();
}

export function loadSettings(env: NodeJS.ProcessEnv = process.env): Part4Settings {
  const lease: LeaseSettings = {
    heartbeatIntervalMs: readInt(env, 'P4_LEASE_HEARTBEAT_MS', 5_000, 500, 60_000),
    leaseTtlMs: readInt(env, 'P4_LEASE_TTL_MS', 20_000, 2_000, 300_000),
  };
  if (lease.heartbeatIntervalMs * 2 > lease.leaseTtlMs) {
    throw new Error('lease heartbeat must fire at least twice per lease TTL');
  }

  return {
    gateway: {
      baseUrl: readString(env, 'P4_GATEWAY_BASE_URL', 'http://127.0.0.1:8099'),
      requestTimeoutMs: readInt(env, 'P4_GATEWAY_TIMEOUT_MS', DEFAULT_GATEWAY_TIMEOUT_MS, 250, MAX_GATEWAY_TIMEOUT_MS),
      maxAttempts: readInt(env, 'P4_GATEWAY_MAX_ATTEMPTS', 3, 1, 6),
      apiKeyEnv: 'P4_GATEWAY_API_KEY',
    },
    export: {
      outputDir: readString(env, 'P4_EXPORT_DIR', '/var/lib/lending/statements'),
      rowsPerFlush: readInt(env, 'P4_EXPORT_ROWS_PER_FLUSH', 500, 1, 10_000),
    },
    lease,
    consumer: {
      topic: readString(env, 'P4_DISBURSEMENT_TOPIC', 'loan.disbursement.requested'),
      groupId: readString(env, 'P4_CONSUMER_GROUP', 'disbursement-worker'),
      maxBufferedMessages: readInt(env, 'P4_CONSUMER_MAX_BUFFERED', 1_000, 1, 50_000),
    },
  };
}
