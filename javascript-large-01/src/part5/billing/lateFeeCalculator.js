'use strict';

/**
 * Late fee and payment allocation rules shown on delivered statements. All
 * money is handled in integer minor units (cents) so totals never accumulate
 * floating point error. These numbers are display-only: the ledger in
 * billing-service remains the system of record.
 */

const DEFAULT_FEE_POLICY = Object.freeze({
  graceDays: 5,
  flatFeeCents: 2500,
  percentBasisPoints: 150, // 1.5% of the overdue balance
  maxFeeCents: 4000,
});

const ALLOCATION_ORDER = Object.freeze(['fees', 'interest', 'principal']);

function assertCents(value, label) {
  if (!Number.isSafeInteger(value) || value < 0) {
    throw new RangeError(`${label} must be a non-negative integer number of cents`);
  }
}

/**
 * Compute the late fee for an overdue balance.
 *
 * No fee is charged inside the grace period or on a zero balance. Otherwise the
 * fee is the larger of the flat fee and the percentage fee, rounded half-up to
 * the cent, and never more than the policy maximum or the balance itself.
 */
function computeLateFee(balanceCents, daysLate, policy = DEFAULT_FEE_POLICY) {
  assertCents(balanceCents, 'balanceCents');
  if (!Number.isInteger(daysLate) || daysLate < 0) {
    throw new RangeError('daysLate must be a non-negative integer');
  }
  if (balanceCents === 0 || daysLate <= policy.graceDays) {
    return 0;
  }

  // Integer arithmetic: (balance * bp + 5000) / 10000 rounds half-up.
  const percentFee = Math.floor((balanceCents * policy.percentBasisPoints + 5000) / 10000);
  const fee = Math.max(policy.flatFeeCents, percentFee);
  return Math.min(fee, policy.maxFeeCents, balanceCents);
}

/**
 * Apply a payment to outstanding buckets in waterfall order: fees first, then
 * interest, then principal. Returns the per-bucket amounts applied, the
 * remaining balances, and any overpayment left after every bucket is cleared.
 */
function allocatePayment(paymentCents, outstanding) {
  assertCents(paymentCents, 'paymentCents');
  let remaining = paymentCents;
  const applied = {};
  const balances = {};

  for (const bucket of ALLOCATION_ORDER) {
    const owed = outstanding[bucket] || 0;
    assertCents(owed, `outstanding.${bucket}`);
    const take = Math.min(owed, remaining);
    applied[bucket] = take;
    balances[bucket] = owed - take;
    remaining -= take;
  }

  return {
    applied,
    balances,
    overpaymentCents: remaining,
    settled: ALLOCATION_ORDER.every((bucket) => balances[bucket] === 0),
  };
}

/**
 * Format cents as a plain decimal string for statement rendering.
 */
function formatCents(cents) {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  return `${sign}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

module.exports = {
  computeLateFee,
  allocatePayment,
  formatCents,
  DEFAULT_FEE_POLICY,
  ALLOCATION_ORDER,
};
