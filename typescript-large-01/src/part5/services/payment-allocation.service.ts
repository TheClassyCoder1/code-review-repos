import {
  AllocationResult,
  Cents,
  Installment,
  PaymentAllocation,
  RepaymentSchedule,
} from '../domain/types';
import { assertCents } from '../util/money';

export interface InstallmentBalance {
  sequence: number;
  fees: Cents;
  interest: Cents;
  principal: Cents;
}

export function outstandingFor(inst: Installment): InstallmentBalance {
  return {
    sequence: inst.sequence,
    fees: inst.feesDue - inst.feesPaid,
    interest: inst.interestDue - inst.interestPaid,
    principal: inst.principalDue - inst.principalPaid,
  };
}

/**
 * Applies incoming payments using the regulatory waterfall: oldest installment first,
 * and within an installment fees, then interest, then principal.
 */
export class PaymentAllocationService {
  /**
   * Pure allocation: computes how `amount` would be split across the schedule
   * without mutating it. Any excess over the total due is returned as `unapplied`
   * so the caller can route it to principal prepayment or a refund.
   */
  allocate(schedule: RepaymentSchedule, amount: Cents): AllocationResult {
    assertCents(amount, 'amount');
    if (amount <= 0) {
      throw new RangeError(`payment amount must be positive, got ${amount}`);
    }

    const ordered = [...schedule.installments].sort((a, b) => a.sequence - b.sequence);
    const allocations: PaymentAllocation[] = [];
    let remaining = amount;

    for (const inst of ordered) {
      if (remaining === 0) {
        break;
      }
      const owed = outstandingFor(inst);
      const toFees = Math.min(remaining, Math.max(0, owed.fees));
      remaining -= toFees;
      const toInterest = Math.min(remaining, Math.max(0, owed.interest));
      remaining -= toInterest;
      const toPrincipal = Math.min(remaining, Math.max(0, owed.principal));
      remaining -= toPrincipal;

      if (toFees + toInterest + toPrincipal > 0) {
        allocations.push({ installmentSequence: inst.sequence, toFees, toInterest, toPrincipal });
      }
    }

    return { allocations, unapplied: remaining };
  }

  /** Returns a new schedule with the allocation applied; the input is left untouched. */
  apply(schedule: RepaymentSchedule, result: AllocationResult): RepaymentSchedule {
    const bySequence = new Map(result.allocations.map((a) => [a.installmentSequence, a]));
    return {
      ...schedule,
      installments: schedule.installments.map((inst) => {
        const a = bySequence.get(inst.sequence);
        if (!a) {
          return { ...inst };
        }
        return {
          ...inst,
          feesPaid: inst.feesPaid + a.toFees,
          interestPaid: inst.interestPaid + a.toInterest,
          principalPaid: inst.principalPaid + a.toPrincipal,
        };
      }),
    };
  }

  totalOutstanding(schedule: RepaymentSchedule): Cents {
    return schedule.installments.reduce((acc, inst) => {
      const o = outstandingFor(inst);
      return acc + o.fees + o.interest + o.principal;
    }, 0);
  }
}
