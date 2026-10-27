export interface PricingTier {
  name: string;
  minScore: number;
  aprBps: number;
  maxTermMonths: number;
  maxAmountCents: number;
}

/** Ordered from best to worst; the first tier whose floor the score clears wins. */
export const PRICING_TIERS: readonly PricingTier[] = [
  { name: 'prime-plus', minScore: 780, aprBps: 649, maxTermMonths: 84, maxAmountCents: 10_000_000 },
  { name: 'prime', minScore: 720, aprBps: 849, maxTermMonths: 72, maxAmountCents: 7_500_000 },
  { name: 'near-prime', minScore: 660, aprBps: 1249, maxTermMonths: 60, maxAmountCents: 4_000_000 },
  { name: 'subprime', minScore: 600, aprBps: 1899, maxTermMonths: 48, maxAmountCents: 1_500_000 },
];

export interface PricedOffer {
  tier: string;
  aprBps: number;
  termMonths: number;
  amountCents: number;
}

export function priceOffer(score: number, requestedAmountCents: number, requestedTermMonths: number): PricedOffer {
  const tier = PRICING_TIERS.find((t) => score >= t.minScore)!;
  return {
    tier: tier.name,
    aprBps: tier.aprBps,
    termMonths: Math.min(requestedTermMonths, tier.maxTermMonths),
    amountCents: Math.min(requestedAmountCents, tier.maxAmountCents),
  };
}
