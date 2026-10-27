import { formatCents } from '../security/html';
import { ReminderRequest } from './reminder-request.validator';

export interface ReminderEmail {
  subject: string;
  html: string;
}

export function buildReminderEmail(req: ReminderRequest, portalOrigin: string): ReminderEmail {
  const due = new Date(`${req.dueDate}T00:00:00Z`).toLocaleDateString('en-US', { timeZone: 'UTC', dateStyle: 'long' });
  const payUrl = `${portalOrigin}/portal/loans/${req.loanId}/pay`;
  const html = [
    '<!doctype html><html><body style="font-family:Arial,sans-serif;color:#1d2433">',
    `<p>Hi ${req.greetingName},</p>`,
    `<p>Your payment of <strong>${formatCents(req.amountDueCents)}</strong> for loan #${req.loanId} is due on ${due}.</p>`,
    `<p><a href="${payUrl}" style="background:#0b5fff;color:#fff;padding:8px 14px;text-decoration:none">Make a payment</a></p>`,
    '<p style="font-size:12px;color:#5b6577">If you have already paid, please ignore this message.</p>',
    '</body></html>',
  ].join('\n');
  return { subject: `Payment reminder for loan #${req.loanId}`, html };
}
