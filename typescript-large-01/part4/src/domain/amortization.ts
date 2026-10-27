import { nthMonthlyDueDate } from './businessCalendar';
import { Installment, MinorUnits } from './types';

export interface AmortizationInput {
  principalMinor: MinorUnits;
  /** Nominal annual rate as a fraction, e.g. 0.129 for 12.9% APR. */
  annualRate: number;
  termMonths: number;
  firstDueDate: string;
}

/**
 * Level monthly payment for a fully amortizing loan, rounded up to the next
 * minor unit so the schedule never under-collects before the final installment.
 */
export function levelPaymentMinor(principalMinor: MinorUnits, annualRate: number, termMonths: number): MinorUnits {
  if (termMonths <= 0) {
    throw new RangeError('term must be at least one month');
  }
  const monthlyRate = annualRate / 12;
  if (monthlyRate === 0) {
    return Math.ceil(principalMinor / termMonths);
  }
  const factor = Math.pow(1 + monthlyRate, termMonths);
  return Math.ceil((principalMinor * monthlyRate * factor) / (factor - 1));
}

/**
 * Builds the full repayment schedule. Interest is computed on the outstanding
 * balance each month and rounded half-up; the last installment absorbs any
 * rounding residue so the balance closes at exactly zero.
 */
export function buildAmortizationSchedule(input: AmortizationInput): Installment[] {
  const { principalMinor, annualRate, termMonths, firstDueDate } = input;
  if (!Number.isInteger(principalMinor) || principalMinor <= 0) {
    throw new RangeError('principal must be a positive integer amount');
  }
  if (!Number.isFinite(annualRate) || annualRate < 0 || annualRate > 1) {
    throw new RangeError('annual rate must be between 0 and 1');
  }
  if (!Number.isInteger(termMonths) || termMonths <= 0) {
    throw new RangeError('term must be a positive whole number of months');
  }

  const payment = levelPaymentMinor(principalMinor, annualRate, termMonths);
  const monthlyRate = annualRate / 12;
  const schedule: Installment[] = [];
  let balance = principalMinor;

  for (let sequence = 1; sequence <= termMonths; sequence++) {
    const interestMinor = Math.round(balance * monthlyRate);
    const isLast = sequence === termMonths;
    const principalPart = isLast ? balance : Math.min(balance, payment - interestMinor);
    balance -= principalPart;
    schedule.push({
      sequence,
      dueDate: nthMonthlyDueDate(firstDueDate, sequence - 1),
      principalMinor: principalPart,
      interestMinor,
      balanceAfterMinor: balance,
    });
  }
  return schedule;
}

/** Outstanding principal once the first `paidInstallments` rows have been settled. */
export function remainingPrincipal(schedule: Installment[], paidInstallments: number): MinorUnits {
  if (paidInstallments <= 0) {
    return schedule.reduce((acc, row) => acc + row.principalMinor, 0);
  }
  const index = Math.min(paidInstallments, schedule.length) - 1;
  return schedule[index].balanceAfterMinor;
}
