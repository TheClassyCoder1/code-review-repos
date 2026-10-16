const CURRENCY_SYMBOLS: Record<string, string> = { USD: '$', CAD: 'CA$', EUR: '€', GBP: '£' };

export function formatAmount(cents: number, currency: string = 'USD'): string {
  const symbol = CURRENCY_SYMBOLS[currency] ?? `${currency} `;
  const sign = cents < 0 ? '-' : '';
  const value = (Math.abs(cents) / 100).toFixed(2);
  return `${sign}${symbol}${value}`;
}

export function sumCents(values: number[]): number {
  return values.reduce((total, value) => total + value, 0);
}

export function parseCents(input: string): number {
  if (!/^\d+$/.test(input)) {
    throw new Error('Amount must be an integer number of cents');
  }
  return Number(input);
}

export function allocateSplit(totalCents: number, clinicianShareBps: number): { clinicianCents: number; platformCents: number } {
  if (!Number.isInteger(totalCents) || totalCents < 0) {
    throw new Error('totalCents must be a non-negative integer');
  }
  if (!Number.isInteger(clinicianShareBps) || clinicianShareBps < 0 || clinicianShareBps > 10_000) {
    throw new Error('clinicianShareBps must be between 0 and 10000');
  }
  const clinicianCents = Math.floor((totalCents * clinicianShareBps) / 10_000);
  return { clinicianCents, platformCents: totalCents - clinicianCents };
}
