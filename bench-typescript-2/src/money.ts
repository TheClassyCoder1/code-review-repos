export function formatCents(cents: number, currency: string): string {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  const whole = Math.floor(abs / 100);
  const fraction = String(abs % 100).padStart(2, '0');
  return `${sign}${whole}.${fraction} ${currency.toUpperCase()}`;
}

export const ZERO_DECIMAL_CURRENCIES = new Set(['jpy', 'krw', 'vnd']);

export function isZeroDecimal(currency: string): boolean {
  return ZERO_DECIMAL_CURRENCIES.has(currency.toLowerCase());
}

export function parseAmountToCents(input: string): number {
  const match = /^(-?)(\d{1,9})(?:\.(\d{1,2}))?$/.exec(input.trim());
  if (!match) {
    throw new Error('invalid amount');
  }
  const fraction = (match[3] ?? '').padEnd(2, '0');
  const cents = Number(match[2]) * 100 + Number(fraction);
  return match[1] === '-' ? -cents : cents;
}
