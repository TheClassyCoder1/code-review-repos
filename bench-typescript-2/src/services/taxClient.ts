import { config } from '../config';
import { httpClient } from '../http';
import { InvoiceLine } from '../types';

export interface TaxQuote {
  taxCents: number;
  jurisdiction: string;
}

interface TaxResponse {
  total_tax_cents: number;
  jurisdiction: string;
}

export async function quoteTax(customerId: string, currency: string, lines: InvoiceLine[]): Promise<TaxQuote> {
  const response = await httpClient.post<TaxResponse>(`${config.taxServiceUrl}/v2/quotes`, {
    customer_reference: customerId,
    currency,
    lines: lines.map((line) => ({ amount: line.amount_cents, quantity: line.quantity })),
  });
  return { taxCents: response.data.total_tax_cents, jurisdiction: response.data.jurisdiction };
}
