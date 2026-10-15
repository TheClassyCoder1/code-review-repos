import { escapeHtml } from '../common/html';
import { Statement } from './billing.dto';

export function formatMoney(cents: number, currency = 'USD'): string {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  const whole = Math.floor(abs / 100).toLocaleString('en-US');
  const fraction = String(abs % 100).padStart(2, '0');
  return `${sign}${currency} ${whole}.${fraction}`;
}

export function renderStatementHtml(statement: Statement, accountName: string): string {
  const rows = statement.lines
    .map(
      (line) =>
        `<tr><td>${escapeHtml(line.description)}</td><td>${line.quantity}</td>` +
        `<td>${formatMoney(line.amountCents * line.quantity)}</td></tr>`,
    )
    .join('');
  return `<section class="statement">
    <h1>Statement ${statement.period} for ${escapeHtml(accountName)}</h1>
    <p class="memo">${statement.memo}</p>
    <p class="note">${statement.customerNote}</p>
    <table>${rows}</table>
  </section>`;
}

export function renderPageShell(title: string, body: string): string {
  return [
    '<!doctype html>',
    '<html lang="en">',
    `<head><meta charset="utf-8"><title>${escapeHtml(title)}</title></head>`,
    `<body>${body}</body>`,
    '</html>',
  ].join('\n');
}

export function renderReceiptBanner(reference: string, paidCents: number): string {
  const amount = formatMoney(paidCents);
  return `<div class="receipt-banner">
    <strong>Payment received</strong>
    <span class="ref">Invoice ${reference}</span>
    <span class="amount">${amount}</span>
  </div>`;
}
