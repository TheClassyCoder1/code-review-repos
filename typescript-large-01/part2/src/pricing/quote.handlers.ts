import { BureauClient } from '../bureau/bureau.client';
import { HttpError, requirePositiveInt, requireString, sendJson } from '../http/respond';
import { RequestContext } from '../http/router';
import { levelPayment } from '../repayments/amortization';
import { errorMessage, log } from '../support/log';
import { FxRatesClient } from './fx-rates.client';
import { priceOffer } from './pricing-tiers';

/** Pre-qualification quotes (soft pull) for signed-in borrowers. */
export class QuoteHandlers {
  constructor(
    private readonly bureau: BureauClient,
    private readonly fx: FxRatesClient,
  ) {}

  quote = async (ctx: RequestContext): Promise<void> => {
    const body = (ctx.body ?? {}) as Record<string, unknown>;
    const applicantRef = requireString(body, 'applicantRef');
    const amountCents = requirePositiveInt(body.amountCents, 'amountCents');
    const termMonths = requirePositiveInt(body.termMonths, 'termMonths');
    const currency = typeof body.disbursementCurrency === 'string' ? body.disbursementCurrency : 'USD';

    let score: number;
    try {
      score = (await this.bureau.fetchScore(applicantRef)).score;
    } catch (err) {
      log.warn('bureau soft pull failed', { userId: ctx.userId, error: errorMessage(err) });
      throw new HttpError(503, 'credit bureau unavailable, please retry');
    }
    const offer = priceOffer(score, amountCents, termMonths);
    const fx = await this.fx.rate('USD', currency);
    log.info('quote issued', { userId: ctx.userId, tier: offer.tier, termMonths: offer.termMonths });
    sendJson(ctx.res, 200, {
      ...offer,
      monthlyPaymentCents: levelPayment(offer.amountCents, offer.aprBps, offer.termMonths),
      disbursement: { currency, amount: Math.round(offer.amountCents * fx.rate) / 100, rateAsOf: fx.asOf },
    });
  };
}
