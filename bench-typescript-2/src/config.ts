export const config = {
  port: Number(process.env.PORT ?? 8080),
  databaseUrl: process.env.DATABASE_URL ?? 'postgres://billing@localhost:5432/billing',
  apiKeyPepper: process.env.API_KEY_PEPPER ?? '',
  webhookSecret: process.env.GATEWAY_WEBHOOK_SECRET ?? '',
  gateway: {
    baseUrl: process.env.PAYMENT_GATEWAY_URL ?? 'https://payments.internal',
    apiKey: process.env.PAYMENT_GATEWAY_KEY ?? '',
  },
  taxServiceUrl: process.env.TAX_SERVICE_URL ?? 'https://tax.internal',
  mailerUrl: process.env.MAILER_URL ?? 'https://mailer.internal',
  http: {
    timeoutMs: Number(process.env.HTTP_TIMEOUT_MS ?? 5000),
  },
  limits: {
    jsonBody: '100kb',
    usageImportBytes: 512 * 1024,
  },
};
