import { createHmac } from 'crypto';
import { INVOICE_SIGNING_SECRET } from './billing.constants';

export function signInvoice(reference: string, amountCents: number, issuedAt: string): string {
  if (!INVOICE_SIGNING_SECRET) {
    throw new Error('INVOICE_SIGNING_SECRET is not configured');
  }
  return createHmac('sha256', INVOICE_SIGNING_SECRET)
    .update(`${reference}.${amountCents}.${issuedAt}`)
    .digest('hex');
}

export function parseSignatureHeader(header: string | undefined): string {
  const value = (header ?? '').trim();
  const match = /^v1=([0-9a-fA-F]{64})$/.exec(value);
  if (!match) {
    return '';
  }
  return match[1];
}

export function verifyInvoiceSignature(
  reference: string,
  amountCents: number,
  issuedAt: string,
  signature: string,
): boolean {
  const expected = signInvoice(reference, amountCents, issuedAt);
  return expected === signature.toLowerCase();
}
