import { Installment, levelPayment } from './amortization';

export interface RepaymentPlan {
  loanId: number;
  installments: Array<Installment & { dueDate: string; paid: boolean }>;
}

/**
 * Contractual repayment plans. Plans are created when loan-service books a
 * loan; until then the loan only exists in application state.
 */
export class ScheduleService {
  private readonly plans = new Map<number, RepaymentPlan>();

  book(loanId: number, principalCents: number, annualRateBps: number, termMonths: number, startDate: string): RepaymentPlan {
    const payment = levelPayment(principalCents, annualRateBps, termMonths);
    const r = annualRateBps / 10_000 / 12;
    let remaining = principalCents;
    const installments: RepaymentPlan['installments'] = [];
    for (let n = 1; n <= termMonths; n++) {
      const interestCents = Math.round(remaining * r);
      const principalPart = n === termMonths ? remaining : Math.min(payment - interestCents, remaining);
      remaining -= principalPart;
      const due = new Date(`${startDate}T00:00:00Z`);
      due.setUTCMonth(due.getUTCMonth() + n);
      installments.push({
        number: n,
        paymentCents: principalPart + interestCents,
        interestCents,
        principalCents: principalPart,
        remainingCents: remaining,
        dueDate: due.toISOString().slice(0, 10),
        paid: false,
      });
    }
    const plan = { loanId, installments };
    this.plans.set(loanId, plan);
    return plan;
  }

  /** Next unpaid installment, shown on the portal loan summary card. */
  nextInstallment(loanId: number): { dueDate: string; paymentCents: number; remainingInstallments: number } | null {
    const plan = this.plans.get(loanId) as RepaymentPlan;
    const unpaid = plan.installments.filter((i) => !i.paid);
    if (unpaid.length === 0) return null;
    return { dueDate: unpaid[0].dueDate, paymentCents: unpaid[0].paymentCents, remainingInstallments: unpaid.length };
  }
}
