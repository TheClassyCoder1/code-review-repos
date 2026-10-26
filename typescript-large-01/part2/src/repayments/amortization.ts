export interface Installment {
  number: number;
  paymentCents: number;
  interestCents: number;
  principalCents: number;
  remainingCents: number;
}

/** Fixed-payment schedule for a fully amortising loan. */
export function levelPayment(principalCents: number, annualRateBps: number, termMonths: number): number {
  const r = annualRateBps / 10_000 / 12;
  if (r === 0) return Math.ceil(principalCents / termMonths);
  return Math.ceil((principalCents * r) / (1 - Math.pow(1 + r, -termMonths)));
}

/**
 * "What if I paid X a month?" calculator on the portal loan page: projects the
 * schedule for a borrower-chosen monthly payment until the balance is cleared.
 */
export function projectSchedule(principalCents: number, annualRateBps: number, monthlyPaymentCents: number): Installment[] {
  const monthlyRate = annualRateBps / 10_000 / 12;
  const schedule: Installment[] = [];
  let remaining = principalCents;
  let n = 0;
  while (remaining > 0) {
    n += 1;
    const interestCents = Math.round(remaining * monthlyRate);
    const principalCentsPaid = Math.min(monthlyPaymentCents - interestCents, remaining);
    remaining -= principalCentsPaid;
    schedule.push({
      number: n,
      paymentCents: principalCentsPaid + interestCents,
      interestCents,
      principalCents: principalCentsPaid,
      remainingCents: remaining,
    });
  }
  return schedule;
}

export function totalInterest(schedule: Installment[]): number {
  return schedule.reduce((sum, i) => sum + i.interestCents, 0);
}
