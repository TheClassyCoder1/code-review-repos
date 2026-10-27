import * as path from 'path';

export interface AppConfig {
  port: number;
  production: boolean;
  sessionSecret: string;
  publicOrigin: string;
  /** Origins that may be used as a post-logout `continue` target. */
  redirectOrigins: string[];
  uploadsDir: string;
  reportsDir: string;
  quotaFile: string;
  storageHost: string;
  storageSigningKey: string;
  fxBaseUrl: string;
  bankFeedBaseUrl: string;
  webhookAllowedHosts: string[];
  partnerDailyQuota: number;
}

const list = (raw: string | undefined, fallback: string[]): string[] =>
  raw ? raw.split(',').map((s) => s.trim()).filter(Boolean) : fallback;

export function loadConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  const production = env.NODE_ENV === 'production';
  const secret = (name: string, devDefault: string): string => {
    const value = env[name];
    if (!value && production) throw new Error(`${name} must be set in production`);
    return value || devDefault;
  };
  const dataDir = env.PORTAL_DATA_DIR ?? path.resolve(process.cwd(), 'var');
  return {
    port: Number(env.PORT ?? 8120),
    production,
    sessionSecret: secret('PORTAL_SESSION_SECRET', 'local-portal-session-secret'),
    publicOrigin: env.PORTAL_PUBLIC_ORIGIN ?? 'http://localhost:8120',
    redirectOrigins: list(env.PORTAL_REDIRECT_ORIGINS, ['http://localhost:8120', 'http://localhost:8089']),
    uploadsDir: path.join(dataDir, 'uploads'),
    reportsDir: env.PORTAL_REPORTS_DIR ?? path.join(dataDir, 'reports'),
    quotaFile: path.join(dataDir, 'partner-quota.json'),
    storageHost: env.PORTAL_STORAGE_HOST ?? 'documents.storage.local',
    storageSigningKey: secret('PORTAL_STORAGE_SIGNING_KEY', 'local-docs-signing-key'),
    fxBaseUrl: env.PORTAL_FX_URL ?? 'http://localhost:8131',
    bankFeedBaseUrl: env.PORTAL_BANK_FEED_URL ?? 'http://localhost:8132',
    webhookAllowedHosts: list(env.PORTAL_WEBHOOK_ALLOWED_HOSTS, ['hooks.partner-dealer.test', 'events.broker-network.test']),
    partnerDailyQuota: Number(env.PORTAL_PARTNER_DAILY_QUOTA ?? 500),
  };
}
