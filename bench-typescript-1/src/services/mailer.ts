import { logger } from '../logger';

export async function sendEmail(to: string, message: { subject: string; html: string }): Promise<void> {
  const response = await fetch(process.env.MAILER_URL ?? 'http://mailer.internal/v1/messages', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ to, subject: message.subject, html: message.html }),
    signal: AbortSignal.timeout(5_000),
  });
  if (!response.ok) {
    logger.warn({ status: response.status }, 'mailer rejected message');
  }
}
