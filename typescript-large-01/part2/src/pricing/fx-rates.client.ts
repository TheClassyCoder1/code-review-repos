export interface FxQuote {
  base: string;
  quote: string;
  rate: number;
  asOf: string;
}

const CURRENCY = /^[A-Z]{3}$/;

/**
 * Treasury FX service, used when a borrower asks for disbursement into a
 * foreign-currency account.
 */
export class FxRatesClient {
  constructor(private readonly baseUrl: string) {}

  async rate(base: string, quote: string): Promise<FxQuote> {
    if (!CURRENCY.test(base) || !CURRENCY.test(quote)) {
      throw new Error('currency codes must be ISO-4217');
    }
    if (base === quote) {
      return { base, quote, rate: 1, asOf: new Date().toISOString() };
    }
    const res = await fetch(`${this.baseUrl}/v1/rates?base=${base}&quote=${quote}`, {
      headers: { Accept: 'application/json' },
    });
    if (!res.ok) {
      throw new Error(`fx service returned ${res.status}`);
    }
    return (await res.json()) as FxQuote;
  }
}
