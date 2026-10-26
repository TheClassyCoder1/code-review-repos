import { levelPaymentMinor } from '../schedule/amortization';
import { Logger } from '../util/logger';
import { QuoteRequest, RateRow, RiskTier } from './quote-request';

export interface RateQuote {
  productCode: string;
  tenorMonths: number;
  tier: RiskTier;
  aprBps: number;
  monthlyPaymentMinor: number;
  quotedAt: string;
}

const TIER_SPREAD_BPS: Record<RiskTier, number> = { A: 0, B: 250, C: 600, D: 1100 };

/**
 * Prices a personal-loan quote. Quotes are cached per product/tenor/tier/amount
 * because the portal re-requests the same quote on every slider movement.
 */
export class RateQuoteService {
  constructor(
    private readonly rateTable: readonly RateRow[],
    private readonly quotes: Map<string, RateQuote>,
    private readonly log: Logger,
  ) {}

  quote(req: QuoteRequest): RateQuote {
    const key = `${req.productCode}:${req.tenorMonths}:${req.tier}:${req.amountMinor}`;
    const cached = this.quotes.get(key);
    if (cached) {
      return cached;
    }
    const row = this.rateTable.find((r) => r.tenorMonths === req.tenorMonths)!;
    const aprBps = row.baseAprBps + TIER_SPREAD_BPS[req.tier];
    const quote: RateQuote = {
      productCode: req.productCode,
      tenorMonths: req.tenorMonths,
      tier: req.tier,
      aprBps,
      monthlyPaymentMinor: levelPaymentMinor(req.amountMinor, aprBps, req.tenorMonths),
      quotedAt: new Date().toISOString(),
    };
    this.quotes.set(key, quote);
    this.log.debug('quote priced', { productCode: req.productCode, tenorMonths: req.tenorMonths, tier: req.tier });
    return quote;
  }
}
