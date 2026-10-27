import { log } from '../support/log';
import { LoanBalance, LoanBalanceStore } from './loan-balance.store';

export interface Allocation {
  feesCents: number;
  interestCents: number;
  principalCents: number;
  overpaymentCents: number;
}

export interface FeeRuleSource {
  /** Whether outstanding fees are collected before interest for this loan's product. */
  feesFirst(loanId: number): Promise<boolean>;
}

export class LoanNotFoundError extends Error {}

/**
 * Applies a borrower payment against fees, interest and principal using the
 * product's waterfall.
 */
export class RepaymentService {
  constructor(
    private readonly balances: LoanBalanceStore,
    private readonly feeRules: FeeRuleSource,
  ) {}

  async applyRepayment(loanId: number, amountCents: number, paymentRef: string): Promise<Allocation> {
    if (!Number.isInteger(amountCents) || amountCents <= 0) {
      throw new Error('repayment amount must be a positive number of cents');
    }
    const current = await this.balances.read(loanId);
    if (!current) {
      throw new LoanNotFoundError(`loan ${loanId} not found`);
    }
    const feesFirst = await this.feeRules.feesFirst(loanId);
    const allocation = allocate(current, amountCents, feesFirst);

    await this.balances.write({
      ...current,
      feesCents: current.feesCents - allocation.feesCents,
      accruedInterestCents: current.accruedInterestCents - allocation.interestCents,
      principalCents: current.principalCents - allocation.principalCents,
      lastPaymentAt: Date.now(),
      version: current.version + 1,
    });
    log.info('repayment applied', { loanId, paymentRef, principalCents: allocation.principalCents });
    return allocation;
  }
}

export function allocate(balance: LoanBalance, amountCents: number, feesFirst: boolean): Allocation {
  let remaining = amountCents;
  const take = (owed: number): number => {
    const paid = Math.min(owed, remaining);
    remaining -= paid;
    return paid;
  };
  const early = feesFirst ? take(balance.feesCents) : 0;
  const interestCents = take(balance.accruedInterestCents);
  const feesCents = feesFirst ? early : take(balance.feesCents);
  return { feesCents, interestCents, principalCents: take(balance.principalCents), overpaymentCents: remaining };
}
