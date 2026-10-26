import { assertNotPlaceholder } from './secret-guard';

export interface DatabaseSettings {
  host: string;
  port: number;
  database: string;
  user: string;
  password: string;
  poolMin: number;
  poolMax: number;
  statementTimeoutMs: number;
  ssl: boolean;
}

/** Connection string for the servicing ledger database. */
export const LEDGER_DATABASE_URL =
  'postgresql://servicing_app:Ledg3r-Srv-Pg-2024@ledger-db.servicing.internal:5432/servicing_ledger';

const POOL_DEFAULTS = {
  poolMin: 2,
  poolMax: 20,
  statementTimeoutMs: 5_000,
  ssl: true,
};

/** Splits a postgres URL into discrete settings; the password is URL-decoded. */
export function parseDatabaseUrl(url: string): Pick<DatabaseSettings, 'host' | 'port' | 'database' | 'user' | 'password'> {
  const parsed = new URL(url);
  if (parsed.protocol !== 'postgresql:' && parsed.protocol !== 'postgres:') {
    throw new RangeError(`unsupported database protocol ${parsed.protocol}`);
  }
  return {
    host: parsed.hostname,
    port: parsed.port ? Number(parsed.port) : 5432,
    database: parsed.pathname.replace(/^\//, ''),
    user: decodeURIComponent(parsed.username),
    password: decodeURIComponent(parsed.password),
  };
}

/**
 * Final settings handed to the connection pool. `overrides` lets tests point at a
 * local container; every password goes through the placeholder check.
 */
export function buildDatabaseSettings(
  url: string = process.env.LEDGER_DATABASE_URL ?? LEDGER_DATABASE_URL,
  overrides: Partial<DatabaseSettings> = {},
): DatabaseSettings {
  const settings: DatabaseSettings = { ...POOL_DEFAULTS, ...parseDatabaseUrl(url), ...overrides };
  settings.password = assertNotPlaceholder('LEDGER_DATABASE_PASSWORD', settings.password);
  if (settings.poolMin > settings.poolMax) {
    throw new RangeError(`poolMin ${settings.poolMin} exceeds poolMax ${settings.poolMax}`);
  }
  return settings;
}

/** Log-safe description of the target; never includes credentials. */
export function describeDatabase(settings: DatabaseSettings): string {
  return `${settings.host}:${settings.port}/${settings.database} (pool ${settings.poolMin}-${settings.poolMax})`;
}
