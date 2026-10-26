import { Cents } from '../domain/types';

/** Banker's rounding of a fractional cent amount to whole cents. */
export function roundHalfEven(value: number): Cents {
  const floor = Math.floor(value);
  const diff = value - floor;
  const epsilon = 1e-9;
  if (diff > 0.5 + epsilon) {
    return floor + 1;
  }
  if (diff < 0.5 - epsilon) {
    return floor;
  }
  return floor % 2 === 0 ? floor : floor + 1;
}

export function sumCents(values: readonly Cents[]): Cents {
  return values.reduce((acc, v) => acc + v, 0);
}

export function assertCents(value: number, field: string): Cents {
  if (!Number.isSafeInteger(value)) {
    throw new RangeError(`${field} must be an integer number of cents, got ${value}`);
  }
  return value;
}

/**
 * Split `total` into `parts` integer shares that differ by at most one cent,
 * with the remainder cents handed to the earliest shares.
 */
export function splitEvenly(total: Cents, parts: number): Cents[] {
  if (parts <= 0 || !Number.isInteger(parts)) {
    throw new RangeError(`parts must be a positive integer, got ${parts}`);
  }
  const base = Math.trunc(total / parts);
  const remainder = total - base * parts;
  const step = Math.sign(remainder);
  const shares: Cents[] = [];
  for (let i = 0; i < parts; i++) {
    shares.push(i < Math.abs(remainder) ? base + step : base);
  }
  return shares;
}

export function formatCents(amount: Cents, currency = 'USD'): string {
  const sign = amount < 0 ? '-' : '';
  const abs = Math.abs(amount);
  const units = Math.floor(abs / 100);
  const minor = String(abs % 100).padStart(2, '0');
  const grouped = units.toLocaleString('en-US');
  return `${sign}${currency} ${grouped}.${minor}`;
}
