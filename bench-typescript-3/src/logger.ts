import pino from 'pino';

export const logger = pino({
  level: process.env.LOG_LEVEL ?? 'info',
  redact: {
    paths: ['headers.authorization', 'headers.cookie', 'headers["x-webhook-signature"]', 'password', '*.password'],
    censor: '[redacted]',
  },
});
