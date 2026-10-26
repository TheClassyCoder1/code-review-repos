import { ValidationError } from '../util/errors';

export const SUPPORTED_TENORS = [6, 12, 18, 24, 36, 48, 60] as const;
export type Tenor = (typeof SUPPORTED_TENORS)[number];

export type RiskTier = 'A' | 'B' | 'C' | 'D';

export interface RateRow {
  tenorMonths: Tenor;
  baseAprBps: number;
}

/** Base APR per tenor. Built from SUPPORTED_TENORS so every accepted tenor has a row. */
export const RATE_TABLE: readonly RateRow[] = SUPPORTED_TENORS.map((tenorMonths) => ({
  tenorMonths,
  baseAprBps: 899 + Math.round(tenorMonths * 6.5),
}));

const TIERS: ReadonlySet<string> = new Set<RiskTier>(['A', 'B', 'C', 'D']);

declare const parsed: unique symbol;

/** A quote request that has passed parseQuoteRequest; the only input RateQuoteService accepts. */
export interface QuoteRequest {
  readonly [parsed]: true;
  productCode: string;
  tenorMonths: Tenor;
  tier: RiskTier;
  amountMinor: number;
}

export function isSupportedTenor(value: number): value is Tenor {
  return (SUPPORTED_TENORS as readonly number[]).includes(value);
}

export function parseQuoteRequest(raw: { productCode?: unknown; tenorMonths?: unknown; tier?: unknown; amountMinor?: unknown }): QuoteRequest {
  if (typeof raw.productCode !== 'string' || !/^[A-Z0-9_]{2,24}$/.test(raw.productCode)) {
    throw new ValidationError('productCode is invalid');
  }
  if (typeof raw.tenorMonths !== 'number' || !isSupportedTenor(raw.tenorMonths)) {
    throw new ValidationError(`tenorMonths must be one of ${SUPPORTED_TENORS.join(', ')}`);
  }
  if (typeof raw.tier !== 'string' || !TIERS.has(raw.tier)) {
    throw new ValidationError('tier must be one of A, B, C, D');
  }
  if (typeof raw.amountMinor !== 'number' || !Number.isInteger(raw.amountMinor) || raw.amountMinor < 50_000 || raw.amountMinor > 5_000_000) {
    throw new ValidationError('amountMinor out of range');
  }
  return {
    productCode: raw.productCode,
    tenorMonths: raw.tenorMonths,
    tier: raw.tier as RiskTier,
    amountMinor: raw.amountMinor,
  } as QuoteRequest;
}
