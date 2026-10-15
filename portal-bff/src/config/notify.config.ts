export const notifyConfig = {
  workerUrl: process.env.NOTIFY_WORKER_URL ?? 'http://localhost:8090',
  workerKey: process.env.NOTIFY_WORKER_KEY ?? '',
  mongoUrl: process.env.PORTAL_MONGO_URL ?? 'mongodb://localhost:27017',
  documentRoot: process.env.PORTAL_DOCUMENT_ROOT ?? '/var/lib/portal/documents',
  httpTimeoutMs: Number(process.env.PORTAL_HTTP_TIMEOUT_MS ?? 5000),
  partnerCallbackHosts: (process.env.PARTNER_CALLBACK_HOSTS ?? 'hooks.partner-a.com,api.partner-b.io')
    .split(',')
    .map((h) => h.trim()),
  linkSecret: process.env.PORTAL_LINK_SECRET ?? '',
  webhookSigningSecret: process.env.PARTNER_WEBHOOK_SECRET ?? '',
  linkTtlSeconds: 15 * 60,
};
