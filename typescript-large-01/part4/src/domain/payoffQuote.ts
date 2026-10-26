import { daysBetween } from './businessCalendar';
import { MinorUnits } from './types';

export interface PayoffInputs {
  outstandingPrincipalMinor: MinorUnits;
  annualRate: number;
  /** Date interest was last accrued to (normally the previous due date). */
  accruedThrough: string;
  unpaidFeesMinor: MinorUnits;
  /** Credits on the account not yet applied to an installment. */
  suspenseCreditMinor: MinorUnits;
}

export interface PayoffQuote {
  payoffDate: string;
  principalMinor: MinorUnits;
  interestMinor: MinorUnits;
  feesMinor: MinorUnits;
  creditAppliedMinor: MinorUnits;
  totalDueMinor: MinorUnits;
}

/**
 * Amount needed to close a loan on `payoffDate`, using actual/365 simple
 * interest from the last accrual date. Suspense credit is applied last and
 * never makes the total negative; any excess is refunded separately.
 */
export function computePayoffQuote(inputs: PayoffInputs, payoffDate: string): PayoffQuote {
  const { outstandingPrincipalMinor, annualRate, accruedThrough, unpaidFeesMinor, suspenseCreditMinor } = inputs;
  if (outstandingPrincipalMinor < 0 || unpaidFeesMinor < 0 || suspenseCreditMinor < 0) {
    throw new RangeError('payoff inputs must be non-negative');
  }
  if (!Number.isFinite(annualRate) || annualRate < 0) {
    throw new RangeError('annual rate must be a non-negative number');
  }
  const days = daysBetween(accruedThrough, payoffDate);
  if (days < 0) {
    throw new RangeError('payoff date cannot be before the last accrual date');
  }

  const interestMinor = Math.round((outstandingPrincipalMinor * annualRate * days) / 365);
  const gross = outstandingPrincipalMinor + interestMinor + unpaidFeesMinor;
  const creditAppliedMinor = Math.min(gross, suspenseCreditMinor);
  return {
    payoffDate,
    principalMinor: outstandingPrincipalMinor,
    interestMinor,
    feesMinor: unpaidFeesMinor,
    creditAppliedMinor,
    totalDueMinor: gross - creditAppliedMinor,
  };
}
