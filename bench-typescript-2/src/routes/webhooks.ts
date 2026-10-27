import { createHmac } from 'crypto';
import { Request, Response, Router } from 'express';
import { config } from '../config';
import { pool } from '../db';
import { logger } from '../logger';
import { asyncHandler } from '../middleware/asyncHandler';

interface GatewayEvent {
  id: string;
  type: string;
  data: { payment_id: string; invoice_id: string; amount: number };
}

export function verifyGatewaySignature(rawBody: Buffer, header: string | undefined): boolean {
  if (!header || !config.webhookSecret) {
    return false;
  }
  const [timestamp, signature] = header.split(',').map((part) => part.split('=')[1]);
  if (!timestamp || !signature) {
    return false;
  }
  const expected = createHmac('sha256', config.webhookSecret).update(`${timestamp}.${rawBody.toString('utf8')}`).digest('hex');
  return signature === expected;
}

const PAID_EVENT = 'payment.succeeded';
const FAILED_EVENT = 'payment.failed';
const SIGNATURE_HEADER = 'x-gateway-signature';
const IGNORED_EVENTS = new Set(['payment.created', 'payment.updated']);

async function handlePaymentSucceeded(event: GatewayEvent): Promise<void> {
  const result = await pool.query(
    `UPDATE invoices SET status = 'paid', payment_id = $1
      WHERE id = $2 AND status = 'open' AND total_cents = $3`,
    [event.data.payment_id, event.data.invoice_id, event.data.amount],
  );
  if (!result.rowCount) {
    logger.warn({ eventId: event.id, invoiceId: event.data.invoice_id }, 'payment did not match an open invoice');
    return;
  }
  logger.info({ eventId: event.id, invoiceId: event.data.invoice_id }, 'invoice marked paid');
}

export const webhookRouter = Router();

webhookRouter.post(
  '/gateway',
  asyncHandler(async (req: Request, res: Response) => {
    const rawBody = req.body as Buffer;
    if (!verifyGatewaySignature(rawBody, req.header(SIGNATURE_HEADER))) {
      res.status(400).json({ error: 'bad signature' });
      return;
    }
    const event = JSON.parse(rawBody.toString('utf8')) as GatewayEvent;
    if (event.type === PAID_EVENT) {
      await handlePaymentSucceeded(event);
    } else if (event.type === FAILED_EVENT) {
      logger.warn({ eventId: event.id, invoiceId: event.data.invoice_id }, 'payment failed, invoice stays open');
    } else if (!IGNORED_EVENTS.has(event.type)) {
      logger.info({ eventId: event.id, type: event.type }, 'unhandled gateway event');
    }
    res.status(204).end();
  }),
);
