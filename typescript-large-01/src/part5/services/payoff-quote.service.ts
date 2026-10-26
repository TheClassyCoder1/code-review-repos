import { Cents, FeeCharge, IsoDate, PayoffQuote, RepaymentSchedule, ServicedLoan } from '../domain/types';
import { BusinessCalendar, daysBetween } from '../util/business-calendar';
import { roundHalfEven } from '../util/money';
import { outstandingFor } from './payment-allocation.service';

/** Payoff quotes stay valid for this many business days. */
const QUOTE_VALIDITY_BUSINESS_DAYS = 10;

/** Day-count basis for daily interest (actual/365). */
const DAYS_PER_YEAR = 365;

export class PayoffQuoteService {
  constructor(private readonly calendar: BusinessCalendar) {}

  /** Daily interest on `principal` at the loan's APR, half-even rounded to the cent. */
  perDiem(principal: Cents, annualRateBps: number): Cents {
    if (principal <= 0 || annualRateBps <= 0) {
      return 0;
    }
    return roundHalfEven((principal * annualRateBps) / 10_000 / DAYS_PER_YEAR);
  }

  /**
   * Payoff = unpaid principal across the whole schedule + interest accrued since the
   * last due date on or before `asOf` + unwaived fees. Interest already billed on past
   * installments is included via their outstanding interest, not re-accrued.
   * `goodThrough` is a business day; the quote's total covers interest up to that date.
   */
  quotePayoff(
    loan: ServicedLoan,
    schedule: RepaymentSchedule,
    fees: readonly FeeCharge[],
    asOf: IsoDate,
  ): PayoffQuote {
    let principal = 0;
    let billedInterest = 0;
    let lastDueOnOrBefore = loan.disbursedOn;

    for (const inst of schedule.installments) {
      const o = outstandingFor(inst);
      principal += o.principal;
      if (inst.dueDate <= asOf) {
        billedInterest += o.interest;
        lastDueOnOrBefore = inst.dueDate;
      }
    }

    const goodThrough = this.calendar.addBusinessDays(asOf, QUOTE_VALIDITY_BUSINESS_DAYS);
    const daily = this.perDiem(principal, loan.annualRateBps);
    const accrualDays = Math.max(0, daysBetween(lastDueOnOrBefore, goodThrough));
    const accruedInterest = billedInterest + daily * accrualDays;
    const outstandingFees = fees
      .filter((f) => f.loanId === loan.loanId && !f.waived)
      .reduce((acc, f) => acc + f.amount, 0);

    return {
      loanId: loan.loanId,
      asOf,
      goodThrough,
      principal,
      accruedInterest,
      outstandingFees,
      total: principal + accruedInterest + outstandingFees,
      perDiem: daily,
    };
  }
}
