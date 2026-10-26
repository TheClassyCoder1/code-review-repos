import { ValidationError } from '../util/errors';
import { Logger } from '../util/logger';

export interface HardshipRequest {
  loanId: string;
  outstandingMinor: number;
  aprBps: number;
  /** Monthly amount the borrower says they can afford, from the hardship form. */
  affordablePaymentMinor: number;
}

export interface HardshipOffer {
  loanId: string;
  aprBps: number;
  paymentMinor: number;
  payoffMonths: number;
  totalInterestMinor: number;
}

/** Builds a hardship repayment offer showing how long payoff takes at what the borrower can afford. */
export class HardshipPlanner {
  constructor(private readonly log: Logger) {}

  offer(req: HardshipRequest): HardshipOffer {
    if (req.outstandingMinor <= 0) {
      throw new ValidationError(`nothing outstanding on ${req.loanId}`);
    }
    if (req.affordablePaymentMinor <= 0) {
      throw new ValidationError(`affordable payment must be positive for ${req.loanId}`);
    }
    const aprBps = req.aprBps;
    const rate = aprBps / 10_000 / 12;

    let balance = req.outstandingMinor;
    let months = 0;
    let interestTotal = 0;
    while (balance > 0) {
      const interest = Math.round(balance * rate);
      interestTotal += interest;
      balance = balance + interest - req.affordablePaymentMinor;
      months++;
    }

    this.log.info('hardship offer computed', { loanId: req.loanId, months, aprBps });
    return {
      loanId: req.loanId,
      aprBps,
      paymentMinor: req.affordablePaymentMinor,
      payoffMonths: months,
      totalInterestMinor: interestTotal,
    };
  }
}
