import { CurrencyCode, MinorUnits } from './types';

const MINOR_DIGITS: Record<CurrencyCode, number> = {
  USD: 2,
  EUR: 2,
  GBP: 2,
  INR: 2,
};

/**
 * Splits `totalMinor` across `weights` so that the parts always sum back to the
 * total. Uses the largest-remainder method; ties go to the earlier index so the
 * result is deterministic for identical inputs.
 */
export function allocateProRata(totalMinor: MinorUnits, weights: number[]): MinorUnits[] {
  if (!Number.isInteger(totalMinor) || totalMinor < 0) {
    throw new RangeError('total must be a non-negative integer amount');
  }
  if (weights.length === 0) {
    return [];
  }
  if (weights.some((w) => !Number.isFinite(w) || w < 0)) {
    throw new RangeError('weights must be finite and non-negative');
  }
  const weightSum = weights.reduce((acc, w) => acc + w, 0);
  if (weightSum === 0) {
    throw new RangeError('at least one weight must be positive');
  }

  const exact = weights.map((w) => (totalMinor * w) / weightSum);
  const parts = exact.map((x) => Math.floor(x));
  let remainder = totalMinor - parts.reduce((acc, p) => acc + p, 0);

  const byFraction = exact
    .map((x, index) => ({ index, fraction: x - Math.floor(x) }))
    .sort((a, b) => b.fraction - a.fraction || a.index - b.index);

  for (let i = 0; remainder > 0; i = (i + 1) % byFraction.length) {
    parts[byFraction[i].index] += 1;
    remainder -= 1;
  }
  return parts;
}

/**
 * Renders a minor-unit amount as a plain decimal string ("-1234.05"), without
 * grouping separators, for CSV exports and gateway payloads.
 */
export function formatMinor(amountMinor: MinorUnits, currency: CurrencyCode): string {
  if (!Number.isSafeInteger(amountMinor)) {
    throw new RangeError('amount must be a safe integer number of minor units');
  }
  const digits = MINOR_DIGITS[currency];
  const negative = amountMinor < 0;
  const absolute = Math.abs(amountMinor).toString().padStart(digits + 1, '0');
  const whole = absolute.slice(0, absolute.length - digits);
  const fraction = absolute.slice(absolute.length - digits);
  const body = digits === 0 ? whole : `${whole}.${fraction}`;
  return negative ? `-${body}` : body;
}

/** Parses a decimal string such as "1234.5" into minor units, rejecting extra precision. */
export function parseMinor(value: string, currency: CurrencyCode): MinorUnits {
  const digits = MINOR_DIGITS[currency];
  const match = /^(-?)(\d+)(?:\.(\d+))?$/.exec(value.trim());
  if (!match) {
    throw new RangeError(`not a decimal amount: "${value}"`);
  }
  const [, sign, whole, fraction = ''] = match;
  if (fraction.length > digits) {
    throw new RangeError(`amount has more than ${digits} fractional digits`);
  }
  const minor = Number.parseInt(whole + fraction.padEnd(digits, '0'), 10);
  if (!Number.isSafeInteger(minor)) {
    throw new RangeError('amount is too large');
  }
  return sign === '-' && minor !== 0 ? -minor : minor;
}
