export interface AppConfig {
  env: string;
  port: number;
  databaseUrl: string;
  jwtSecret: string;
  kycRoot: string;
  payrailBaseUrl: string;
  payrailWebhookSecret?: string;
  webhookHosts: string[];
  publicBaseUrl: string;
}

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable ${name}`);
  }
  return value;
}

export function loadConfig(): AppConfig {
  const env = process.env.NODE_ENV ?? 'development';
  const payrailWebhookSecret = process.env.PAYRAIL_WEBHOOK_SECRET;
  if (env !== 'development' && env !== 'test' && !payrailWebhookSecret) {
    throw new Error('PAYRAIL_WEBHOOK_SECRET must be set outside development and test');
  }
  return {
    env,
    port: Number.parseInt(process.env.PORT ?? '8080', 10),
    databaseUrl: requireEnv('DATABASE_URL'),
    jwtSecret: requireEnv('JWT_SECRET'),
    kycRoot: process.env.KYC_ROOT ?? '/var/lib/seller-onboarding/kyc',
    payrailBaseUrl: process.env.PAYRAIL_BASE_URL ?? 'https://api.payrail.example.com',
    payrailWebhookSecret,
    webhookHosts: (process.env.PAYOUT_WEBHOOK_HOSTS ?? '').split(',').map((h) => h.trim()).filter(Boolean),
    publicBaseUrl: process.env.PUBLIC_BASE_URL ?? 'https://sellers.example.com',
  };
}

export const config = loadConfig();
