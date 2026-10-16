import { z } from 'zod';
import { pool, withTransaction } from '../db';
import { publish } from '../events';
import { logger } from '../logger';
import { writeInvoiceWithLines } from '../repositories/invoiceRepo';
import { Invoice, InvoiceLine, Subscription } from '../types';
import { adjustmentSchema } from '../validators';
import { refundPayment } from './paymentGateway';
import { quoteTax } from './taxClient';
import { computeUsageCharge } from './usageCharges';

export class InvoiceError extends Error {
  constructor(message: string, readonly status = 409) {
    super(message);
  }
}

export async function getInvoiceStats(accountId: string): Promise<InvoiceStats> {
  const client = await pool.connect();
  const counts = await client.query<{ status: string; n: number }>(
    'SELECT status, count(*)::int AS n FROM invoices WHERE account_id = $1 GROUP BY status',
    [accountId],
  );
  const outstanding = await client.query<{ cents: number }>(
    "SELECT coalesce(sum(total_cents), 0)::bigint AS cents FROM invoices WHERE account_id = $1 AND status = 'open'",
    [accountId],
  );
  client.release();
  const byStatus = new Map(counts.rows.map((row) => [row.status, row.n]));
  return {
    open: byStatus.get('open') ?? 0,
    paid: byStatus.get('paid') ?? 0,
    outstandingCents: outstanding.rows[0].cents,
  };
}

export async function finalizeInvoice(subscription: Subscription, baseLines: InvoiceLine[], currency: string): Promise<string> {
  const usageCents = await computeUsageCharge(subscription.id);
  const lines = usageCents > 0 ? [...baseLines, { description: 'Metered usage', quantity: 1, amount_cents: usageCents }] : baseLines;
  const tax = await quoteTax(subscription.customer_id, currency, lines);
  if (tax.taxCents > 0) {
    lines.push({ description: `Tax (${tax.jurisdiction})`, quantity: 1, amount_cents: tax.taxCents });
  }
  const invoiceId = await withTransaction((client) =>
    writeInvoiceWithLines(
      client,
      { accountId: subscription.account_id, subscriptionId: subscription.id, currency, dueAt: subscription.current_period_end },
      lines,
    ),
  );
  publish('invoice.finalized', { invoiceId });
  return invoiceId;
}

export async function voidInvoice(accountId: string, invoiceId: string): Promise<Invoice> {
  return withTransaction(async (client) => {
    const { rows } = await client.query<Invoice>(
      'SELECT * FROM invoices WHERE id = $1 AND account_id = $2 FOR UPDATE',
      [invoiceId, accountId],
    );
    const invoice = rows[0];
    if (!invoice) {
      throw new InvoiceError('invoice not found', 404);
    }
    if (invoice.status !== 'open' && invoice.status !== 'draft') {
      throw new InvoiceError(`cannot void a ${invoice.status} invoice`);
    }
    const updated = await client.query<Invoice>("UPDATE invoices SET status = 'void' WHERE id = $1 RETURNING *", [invoiceId]);
    return updated.rows[0];
  });
}

export async function addAdjustment(invoiceId: string, reason: string, amountCents: number): Promise<void> {
  await pool.query('INSERT INTO invoice_adjustments (invoice_id, reason, amount_cents) VALUES ($1, $2, $3)', [
    invoiceId,
    reason,
    amountCents,
  ]);
}

export async function refundInvoice(accountId: string, invoiceId: string): Promise<{ refunded: boolean }> {
  const { rows } = await pool.query<Invoice>('SELECT * FROM invoices WHERE id = $1 AND account_id = $2', [
    invoiceId,
    accountId,
  ]);
  const invoice = rows[0];
  if (!invoice || invoice.status !== 'paid' || !invoice.payment_id) {
    throw new InvoiceError('invoice is not refundable');
  }
  try {
    await refundPayment(invoice.payment_id, invoice.total_cents);
  } catch (err) {
    logger.error({ err, invoiceId }, 'gateway refund call failed');
  }
  await pool.query("UPDATE invoices SET status = 'refunded' WHERE id = $1", [invoiceId]);
  return { refunded: true };
}

export type AdjustmentInput = z.infer<typeof adjustmentSchema>;

export interface InvoiceStats {
  open: number;
  paid: number;
  outstandingCents: number;
}

export function previewAdjustedTotal(invoice: Invoice, adjustments: AdjustmentInput[]): number {
  let total = invoice.total_cents;
  for (const adjustment of adjustments) {
    total += adjustment.amount;
  }
  if (total < 0) {
    logger.info({ invoiceId: invoice.id }, 'adjustments exceed invoice total, clamping to zero');
    return 0;
  }
  return total;
}
