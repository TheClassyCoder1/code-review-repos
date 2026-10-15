import path from 'path';

const REQUIRED_PRODUCTION_SECRETS = ['SESSION_SECRET', 'PORTAL_JWT_SECRET', 'WEBHOOK_SECRET', 'ESIGN_API_KEY'];

export const config = {
  env: process.env.NODE_ENV ?? 'development',
  port: Number(process.env.PORT ?? 3000),
  publicBaseUrl: process.env.PUBLIC_BASE_URL ?? 'http://localhost:3000',
  sessionSecret: process.env.SESSION_SECRET ?? 'local-dev-session-secret',
  webhookSecret: process.env.WEBHOOK_SECRET ?? 'local-dev-webhook-secret',
  uploadDir: process.env.UPLOAD_DIR ?? '/var/lib/recruiting/uploads',
  exportDir: process.env.EXPORT_DIR ?? '/var/lib/recruiting/exports',
  pipelineTemplatePath: path.resolve(__dirname, '../config/pipeline.yaml'),
  jobFeedTimeoutMs: 5000,
  calendar: {
    baseUrl: process.env.CALENDAR_BASE_URL ?? 'https://calendar.internal.example.com',
    apiKey: process.env.CALENDAR_API_KEY ?? '',
    allowedHosts: ['www.googleapis.com', 'graph.microsoft.com', 'calendar.internal.example.com'],
    timeoutMs: 4000,
  },
  esign: {
    baseUrl: process.env.ESIGN_BASE_URL ?? 'https://api.esign.example.com',
    accountId: process.env.ESIGN_ACCOUNT_ID ?? '',
    apiKey: process.env.ESIGN_API_KEY ?? '',
  },
  backgroundCheck: {
    baseUrl: process.env.BGCHECK_BASE_URL ?? 'https://api.screening.example.com',
  },
};

export function assertProductionSecrets(): void {
  if (config.env !== 'production') {
    return;
  }
  for (const name of REQUIRED_PRODUCTION_SECRETS) {
    if (!process.env[name]) {
      throw new Error(`${name} must be set in production`);
    }
  }
}
