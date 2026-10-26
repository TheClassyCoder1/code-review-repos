import { HttpError, requirePositiveInt, requireString, sendJson } from '../http/respond';
import { RequestContext } from '../http/router';
import { IdempotencyCache } from '../idempotency/idempotency-cache';
import { projectSchedule, totalInterest } from './amortization';
import { LoanBalanceStore } from './loan-balance.store';
import { LoanNotFoundError, RepaymentService } from './repayment.service';
import { ScheduleService } from './schedule.service';

export class RepaymentHandlers {
  constructor(
    private readonly repayments: RepaymentService,
    private readonly schedules: ScheduleService,
    private readonly balances: LoanBalanceStore,
    private readonly idempotency: IdempotencyCache,
  ) {}

  pay = async (ctx: RequestContext): Promise<void> => {
    const loanId = this.ownedLoanId(ctx);
    const key = ctx.req.headers['idempotency-key'];
    if (typeof key !== 'string' || key.length < 8 || key.length > 128) {
      throw new HttpError(400, 'Idempotency-Key header is required');
    }
    const cacheKey = `${ctx.userId}:${key}`;
    const cached = this.idempotency.lookup(cacheKey);
    if (cached) {
      sendJson(ctx.res, cached.status, cached.body);
      return;
    }
    const amountCents = requirePositiveInt((ctx.body as Record<string, unknown>)?.amountCents, 'amountCents');
    const paymentRef = requireString(ctx.body, 'paymentRef');
    try {
      const allocation = await this.repayments.applyRepayment(loanId, amountCents, paymentRef);
      const body = { loanId, paymentRef, allocation };
      this.idempotency.store(cacheKey, 201, body);
      sendJson(ctx.res, 201, body);
    } catch (err) {
      if (err instanceof LoanNotFoundError) throw new HttpError(404, 'loan not found');
      throw err;
    }
  };

  next = async (ctx: RequestContext): Promise<void> => {
    const loanId = this.ownedLoanId(ctx);
    sendJson(ctx.res, 200, { loanId, next: this.schedules.nextInstallment(loanId) });
  };

  whatIf = async (ctx: RequestContext): Promise<void> => {
    const loanId = this.ownedLoanId(ctx);
    const body = (ctx.body ?? {}) as Record<string, unknown>;
    const monthlyPaymentCents = requirePositiveInt(body.monthlyPaymentCents, 'monthlyPaymentCents');
    const annualRateBps = requirePositiveInt(body.annualRateBps, 'annualRateBps');
    const balance = await this.balances.read(loanId);
    if (!balance) throw new HttpError(404, 'loan not found');
    const schedule = projectSchedule(balance.principalCents, annualRateBps, monthlyPaymentCents);
    sendJson(ctx.res, 200, { loanId, months: schedule.length, totalInterestCents: totalInterest(schedule) });
  };

  private ownedLoanId(ctx: RequestContext): number {
    const loanId = requirePositiveInt(ctx.params.loanId, 'loanId');
    if (this.balances.ownerOf(loanId) !== ctx.userId) throw new HttpError(404, 'loan not found');
    return loanId;
  }
}
