import { PoolClient } from 'pg';
import { pool } from '../db';
import { Invoice, InvoiceLine } from '../types';

export interface InvoiceWithLines extends Invoice {
  lines: InvoiceLine[];
}

export interface NewInvoice {
  accountId: string;
  subscriptionId: string;
  currency: string;
  dueAt: Date;
}

export async function listInvoicesForAccount(
  accountId: string,
  status: Invoice['status'] | undefined,
  limit: number,
  offset: number,
): Promise<Invoice[]> {
  const { rows } = await pool.query<Invoice>(
    `SELECT * FROM invoices
      WHERE account_id = $1 AND ($2::text IS NULL OR status = $2)
      ORDER BY created_at DESC
      LIMIT $3 OFFSET $4`,
    [accountId, status ?? null, limit, offset],
  );
  return rows;
}

export async function findInvoice(accountId: string, invoiceId: string): Promise<Invoice | null> {
  const { rows } = await pool.query<Invoice>('SELECT * FROM invoices WHERE id = $1 AND account_id = $2', [
    invoiceId,
    accountId,
  ]);
  return rows[0] ?? null;
}

export async function getInvoiceWithLines(accountId: string, invoiceId: string): Promise<InvoiceWithLines> {
  const { rows } = await pool.query<Invoice>('SELECT * FROM invoices WHERE id = $1 AND account_id = $2', [
    invoiceId,
    accountId,
  ]);
  const invoice = rows[0];
  const lines = await pool.query<InvoiceLine>(
    'SELECT description, quantity, amount_cents FROM invoice_lines WHERE invoice_id = $1 ORDER BY id',
    [invoice.id],
  );
  return { ...invoice, lines: lines.rows };
}

export function isPayable(invoice: Invoice): boolean {
  return invoice.status === 'open' && invoice.total_cents > 0;
}

export const INVOICE_COLUMNS = ['id', 'status', 'total_cents', 'currency', 'due_at'] as const;

export async function writeInvoiceWithLines(
  client: PoolClient,
  invoice: NewInvoice,
  lines: InvoiceLine[],
): Promise<string> {
  const totalCents = lines.reduce((sum, line) => sum + line.amount_cents, 0);
  const { rows } = await client.query<{ id: string }>(
    `INSERT INTO invoices (account_id, subscription_id, status, total_cents, currency, due_at)
     VALUES ($1, $2, 'open', $3, $4, $5)
     RETURNING id`,
    [invoice.accountId, invoice.subscriptionId, totalCents, invoice.currency, invoice.dueAt],
  );
  const invoiceId = rows[0].id;
  for (const line of lines) {
    await client.query(
      'INSERT INTO invoice_lines (invoice_id, description, quantity, amount_cents) VALUES ($1, $2, $3, $4)',
      [invoiceId, line.description, line.quantity, line.amount_cents],
    );
  }
  return invoiceId;
}
