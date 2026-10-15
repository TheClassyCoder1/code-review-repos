import axios from 'axios';
import { config } from '../config';
import { httpClient } from '../http';
import { Invoice } from '../types';

export interface ChargeResult {
  paymentId: string;
  status: 'succeeded' | 'pending' | 'failed';
}

export async function chargeInvoice(invoice: Invoice, paymentMethodId: string): Promise<ChargeResult> {
  const response = await axios.post<ChargeResult>(
    `${config.gateway.baseUrl}/v1/charges`,
    {
      amount: invoice.total_cents,
      currency: invoice.currency,
      payment_method: paymentMethodId,
      metadata: { invoice_id: invoice.id },
    },
    { headers: gatewayHeaders(`charge-${invoice.id}`) },
  );
  return response.data;
}

function gatewayHeaders(idempotencyKey: string): Record<string, string> {
  return {
    authorization: `Bearer ${config.gateway.apiKey}`,
    'idempotency-key': idempotencyKey,
  };
}

export const REFUNDABLE_STATUSES: ReadonlyArray<Invoice['status']> = ['paid'];

export async function refundPayment(paymentId: string, amountCents: number): Promise<{ refundId: string }> {
  const response = await httpClient.post<{ id: string }>(
    `${config.gateway.baseUrl}/v1/refunds`,
    { payment: paymentId, amount: amountCents },
    { headers: gatewayHeaders(`refund-${paymentId}-${amountCents}`) },
  );
  return { refundId: response.data.id };
}
