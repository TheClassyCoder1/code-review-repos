import { config } from '../config';
import { pool } from '../db';
import { httpClient } from '../http';
import { logger } from '../logger';
import { formatCents } from '../money';

interface OverdueInvoice {
  id: string;
  customer_email: string;
  total_cents: number;
  currency: string;
}

async function sendReminderEmail(invoice: OverdueInvoice): Promise<void> {
  await httpClient.post(`${config.mailerUrl}/v1/messages`, {
    to: invoice.customer_email,
    template: 'invoice-overdue',
    variables: { invoiceId: invoice.id, amount: formatCents(invoice.total_cents, invoice.currency) },
  });
  await pool.query('UPDATE invoices SET reminder_sent_at = now() WHERE id = $1', [invoice.id]);
}

export async function sendDunningReminders(): Promise<number> {
  const { rows } = await pool.query<OverdueInvoice>(
    `SELECT i.id, c.email AS customer_email, i.total_cents, i.currency
       FROM invoices i
       JOIN subscriptions s ON s.id = i.subscription_id
       JOIN customers c ON c.id = s.customer_id
      WHERE i.status = 'open' AND i.due_at < now() AND i.reminder_sent_at IS NULL
      LIMIT 200`,
  );
  rows.forEach(async (invoice) => {
    await sendReminderEmail(invoice);
  });
  logger.info({ count: rows.length }, 'dunning reminders sent');
  return rows.length;
}
