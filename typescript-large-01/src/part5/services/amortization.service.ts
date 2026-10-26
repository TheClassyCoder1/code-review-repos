import { Cents, Installment, RepaymentSchedule, ServicedLoan } from '../domain/types';
import { BusinessCalendar } from '../util/business-calendar';
import { assertCents, roundHalfEven } from '../util/money';

const MAX_TERM_MONTHS = 480;

/**
 * Level-payment amortization for fixed-rate installment loans.
 * Interest accrues monthly on the outstanding principal at APR / 12.
 */
export class AmortizationService {
  constructor(private readonly calendar: BusinessCalendar) {}

  /**
   * Standard annuity payment rounded half-even to the cent. A zero-rate loan
   * degenerates to straight-line principal repayment.
   */
  computeMonthlyPayment(principal: Cents, annualRateBps: number, termMonths: number): Cents {
    assertCents(principal, 'principal');
    if (principal <= 0) {
      throw new RangeError(`principal must be positive, got ${principal}`);
    }
    if (!Number.isInteger(termMonths) || termMonths < 1 || termMonths > MAX_TERM_MONTHS) {
      throw new RangeError(`termMonths must be between 1 and ${MAX_TERM_MONTHS}, got ${termMonths}`);
    }
    if (!Number.isFinite(annualRateBps) || annualRateBps < 0) {
      throw new RangeError(`annualRateBps must be non-negative, got ${annualRateBps}`);
    }

    if (annualRateBps === 0) {
      return Math.ceil(principal / termMonths);
    }

    const monthlyRate = annualRateBps / 10_000 / 12;
    const growth = Math.pow(1 + monthlyRate, termMonths);
    const payment = (principal * monthlyRate * growth) / (growth - 1);
    return roundHalfEven(payment);
  }

  /**
   * Full schedule. Each installment's interest is computed on the running balance;
   * the last installment absorbs whatever principal the rounded payments left over,
   * so principal across the schedule always sums exactly to the loan amount.
   */
  buildSchedule(loan: ServicedLoan): RepaymentSchedule {
    const payment = this.computeMonthlyPayment(loan.principal, loan.annualRateBps, loan.termMonths);
    const monthlyRate = loan.annualRateBps / 10_000 / 12;
    const installments: Installment[] = [];
    let balance = loan.principal;

    for (let seq = 1; seq <= loan.termMonths; seq++) {
      const interest = roundHalfEven(balance * monthlyRate);
      const isLast = seq === loan.termMonths;
      const principalPart = isLast ? balance : Math.min(balance, payment - interest);
      installments.push({
        sequence: seq,
        dueDate: this.calendar.nthDueDate(loan.firstDueDate, seq),
        principalDue: principalPart,
        interestDue: interest,
        feesDue: 0,
        principalPaid: 0,
        interestPaid: 0,
        feesPaid: 0,
      });
      balance -= principalPart;
    }

    return { loanId: loan.loanId, monthlyPayment: payment, installments };
  }

  /** Remaining principal after the first `paidInstallments` scheduled installments. */
  scheduledBalanceAfter(schedule: RepaymentSchedule, paidInstallments: number): Cents {
    return schedule.installments
      .slice(paidInstallments)
      .reduce((acc, inst) => acc + inst.principalDue, 0);
  }
}
