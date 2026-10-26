import { daysBetween } from './businessCalendar';
import { MinorUnits, RepaymentEntry } from './types';

export interface LateFeePolicy {
  graceDays: number;
  /** Flat fee charged once the grace period is exceeded. */
  flatFeeMinor: MinorUnits;
  /** Daily penalty rate applied to the unpaid amount after grace, e.g. 0.0005. */
  dailyRate: number;
  /** Upper bound on the total late fee for a single installment. */
  capMinor: MinorUnits;
}

/**
 * Late fee for one installment as of `asOfIso`. Nothing is charged while the
 * installment is fully paid or still inside its grace window; a partial
 * payment reduces the base the daily penalty accrues on.
 */
export function computeLateFee(entry: RepaymentEntry, asOfIso: string, policy: LateFeePolicy): MinorUnits {
  const unpaid = entry.amountDueMinor - entry.amountPaidMinor;
  if (unpaid <= 0) {
    return 0;
  }

  if (policy.graceDays < 0 || policy.dailyRate < 0 || policy.capMinor < 0) {
    throw new RangeError('late fee policy values must be non-negative');
  }

  const daysLate = daysBetween(entry.dueDate, asOfIso);
  if (daysLate <= policy.graceDays) {
    return 0;
  }

  const penaltyDays = daysLate - policy.graceDays;
  const accrued = Math.round(unpaid * policy.dailyRate * penaltyDays);
  const total = policy.flatFeeMinor + accrued;
  return Math.min(total, policy.capMinor);
}

/**
 * Applies a payment to installments in due-date order, oldest first. Returns
 * the updated entries and whatever amount was left over after every
 * installment was covered.
 */
export function applyPaymentWaterfall(
  entries: RepaymentEntry[],
  paymentMinor: MinorUnits,
  paidOnIso: string,
): { entries: RepaymentEntry[]; unappliedMinor: MinorUnits } {
  if (!Number.isInteger(paymentMinor) || paymentMinor < 0) {
    throw new RangeError('payment must be a non-negative integer amount');
  }
  const ordered = [...entries].sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  let available = paymentMinor;

  const updated = ordered.map((entry) => {
    const outstanding = entry.amountDueMinor - entry.amountPaidMinor;
    if (available === 0 || outstanding <= 0) {
      return entry;
    }
    const applied = Math.min(outstanding, available);
    available -= applied;
    const amountPaidMinor = entry.amountPaidMinor + applied;
    return {
      ...entry,
      amountPaidMinor,
      paidOn: amountPaidMinor >= entry.amountDueMinor ? paidOnIso : entry.paidOn,
    };
  });

  return { entries: updated, unappliedMinor: available };
}
