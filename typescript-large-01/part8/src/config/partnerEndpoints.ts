export interface PartnerEndpoint {
  readonly host: string;
  readonly port: number;
}

/**
 * Fixed set of partner endpoints the ops console may probe. Changes go through
 * review here; nothing is taken from request input.
 */
const PARTNER_ENDPOINTS: Readonly<Record<string, PartnerEndpoint>> = Object.freeze({
  'credit-bureau': Object.freeze({ host: 'bureau-gw.internal.lending.local', port: 443 }),
  'payment-gateway': Object.freeze({ host: 'payments-gw.internal.lending.local', port: 8443 }),
  'ledger-core': Object.freeze({ host: 'ledger.internal.lending.local', port: 5432 }),
});

/** Returns the configured endpoint for a partner key, or throws for anything not listed. */
export function resolvePartnerEndpoint(partnerKey: string): PartnerEndpoint {
  if (!Object.prototype.hasOwnProperty.call(PARTNER_ENDPOINTS, partnerKey)) {
    throw new Error('unknown partner');
  }
  return PARTNER_ENDPOINTS[partnerKey];
}
