export interface ScheduleLine {
  period: number;
  paymentMinor: number;
  interestMinor: number;
  principalMinor: number;
  closingBalanceMinor: number;
}

export interface ScheduleTerms {
  principalMinor: number;
  aprBps: number;
  paymentMinor: number;
}

export function monthlyRate(aprBps: number): number {
  return aprBps / 10_000 / 12;
}

/** Standard annuity payment for a given principal, APR and term. */
export function levelPaymentMinor(principalMinor: number, aprBps: number, termMonths: number): number {
  const r = monthlyRate(aprBps);
  if (r === 0) {
    return Math.ceil(principalMinor / termMonths);
  }
  return Math.ceil((principalMinor * r) / (1 - Math.pow(1 + r, -termMonths)));
}

/**
 * Builds a repayment schedule by rolling the balance forward month by month
 * until it is paid off. The final line absorbs any rounding residue.
 */
export function buildSchedule(terms: ScheduleTerms): ScheduleLine[] {
  const r = monthlyRate(terms.aprBps);
  const lines: ScheduleLine[] = [];
  let balance = terms.principalMinor;
  let period = 0;
  while (balance > 0) {
    period++;
    const interest = Math.round(balance * r);
    const payment = Math.min(terms.paymentMinor, balance + interest);
    const principal = payment - interest;
    balance -= principal;
    lines.push({
      period,
      paymentMinor: payment,
      interestMinor: interest,
      principalMinor: principal,
      closingBalanceMinor: balance,
    });
  }
  return lines;
}

export function totalInterestMinor(lines: ScheduleLine[]): number {
  return lines.reduce((sum, l) => sum + l.interestMinor, 0);
}
