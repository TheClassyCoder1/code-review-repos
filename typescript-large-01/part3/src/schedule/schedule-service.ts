import { ValidationError } from '../util/errors';
import { buildSchedule, levelPaymentMinor, monthlyRate, ScheduleLine, ScheduleTerms, totalInterestMinor } from './amortization';

export interface ScheduleRequest {
  loanId: string;
  principalMinor: number;
  aprBps: number;
  termMonths?: number;
  paymentMinor?: number;
}

const MAX_PERIODS = 600;

/**
 * Validates schedule terms before any schedule is built. A payment has to
 * cover more than the first month's interest, otherwise the balance never
 * goes down; with that satisfied the balance strictly decreases every period
 * (interest only shrinks as the balance does).
 */
export function validateTerms(req: ScheduleRequest): ScheduleTerms {
  if (!Number.isInteger(req.principalMinor) || req.principalMinor <= 0) {
    throw new ValidationError(`principal must be a positive integer for ${req.loanId}`);
  }
  if (!Number.isInteger(req.aprBps) || req.aprBps < 0 || req.aprBps > 9_999) {
    throw new ValidationError(`APR out of range for ${req.loanId}`);
  }
  let payment: number;
  if (req.paymentMinor !== undefined) {
    payment = req.paymentMinor;
  } else if (req.termMonths !== undefined && Number.isInteger(req.termMonths) && req.termMonths > 0 && req.termMonths <= MAX_PERIODS) {
    payment = levelPaymentMinor(req.principalMinor, req.aprBps, req.termMonths);
  } else {
    throw new ValidationError(`either a payment or a term of 1..${MAX_PERIODS} months is required for ${req.loanId}`);
  }
  const firstInterest = Math.round(req.principalMinor * monthlyRate(req.aprBps));
  if (!Number.isInteger(payment) || payment <= firstInterest) {
    throw new ValidationError(`payment does not cover first-period interest for ${req.loanId}`);
  }
  if (Math.ceil(req.principalMinor / (payment - firstInterest)) > MAX_PERIODS) {
    throw new ValidationError(`payment too small: schedule would exceed ${MAX_PERIODS} periods for ${req.loanId}`);
  }
  return { principalMinor: req.principalMinor, aprBps: req.aprBps, paymentMinor: payment };
}

export class ScheduleService {
  quote(req: ScheduleRequest): { paymentMinor: number; totalInterestMinor: number; lines: ScheduleLine[] } {
    const terms = validateTerms(req);
    const lines = buildSchedule(terms);
    return { paymentMinor: terms.paymentMinor, totalInterestMinor: totalInterestMinor(lines), lines };
  }
}
