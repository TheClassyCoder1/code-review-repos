export default () => ({
  port: parseInt(process.env.PORT ?? '3000', 10),
  database: {
    url: process.env.DATABASE_URL ?? 'postgres://telehealth:telehealth@localhost:5432/telehealth',
  },
  redis: {
    url: process.env.REDIS_URL ?? 'redis://localhost:6379',
  },
  pharmacy: {
    baseUrl: process.env.PHARMACY_API_URL ?? 'https://api.rxpartner.example.com',
    apiKey: process.env.PHARMACY_API_KEY,
    timeoutMs: parseInt(process.env.PHARMACY_TIMEOUT_MS ?? '5000', 10),
    formularyPath: process.env.FORMULARY_PATH ?? 'config/formulary.json',
    callbackSecret: process.env.PHARMACY_CALLBACK_SECRET,
    unpaidOrderTtlMinutes: 60,
  },
  payments: {
    webhookSecret: process.env.PAYMENTS_WEBHOOK_SECRET,
    webhookToleranceSeconds: 300,
  },
  recordings: {
    dir: process.env.RECORDINGS_DIR ?? '/var/lib/telehealth/recordings',
    maxTranscriptBytes: 2 * 1024 * 1024,
  },
  reminders: {
    leadMinutes: 60,
    lockTtlMs: 55_000,
  },
});
