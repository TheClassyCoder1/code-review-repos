'use strict';

const { collectionsConfig } = require('./config');
const { toCents, fromCents, roundHalfEven } = require('./money');
const { daysBetween } = require('./businessDays');

/**
 * Delinquency rules: days past due, aging buckets, and late fees. These are
 * pure functions over plain installment records so they can be exercised from
 * the nightly batch, the portal summary endpoint and unit tests alike.
 *
 * Installment shape: { loanId, dueDate: 'YYYY-MM-DD', amountDue, amountPaid }
 */

const BUCKETS = Object.freeze([
  { name: 'current', minDays: 0, maxDays: 0 },
  { name: '1-29', minDays: 1, maxDays: 29 },
  { name: '30-59', minDays: 30, maxDays: 59 },
  { name: '60-89', minDays: 60, maxDays: 89 },
  { name: '90+', minDays: 90, maxDays: Infinity },
]);

/**
 * Days an installment is past due as of `asOfDay`, after the grace period.
 * Fully paid installments and installments not yet due are never past due.
 */
function daysPastDue(installment, asOfDay, graceDays = collectionsConfig.delinquency.gracePeriodDays) {
  if (!installment || typeof installment.dueDate !== 'string') {
    throw new TypeError('installment with a dueDate is required');
  }
  const outstanding = toCents(installment.amountDue) - toCents(installment.amountPaid || 0);
  if (outstanding <= 0) {
    return 0;
  }

  const elapsed = daysBetween(installment.dueDate, asOfDay);
  if (elapsed <= graceDays) {
    return 0;
  }
  return elapsed;
}

/**
 * Map a days-past-due figure onto its aging bucket name.
 */
function bucketFor(days) {
  if (!Number.isInteger(days) || days < 0) {
    throw new RangeError(`days past due must be a non-negative integer: ${days}`);
  }
  const bucket = BUCKETS.find((b) => days >= b.minDays && days <= b.maxDays);
  return bucket.name;
}

/**
 * Late fee for a missed installment: the greater of a flat fee and a
 * percentage of the outstanding amount, capped, and never more than the
 * outstanding amount itself. Returns a decimal amount.
 */
function lateFee(installment, rules = collectionsConfig.delinquency) {
  const outstandingCents = toCents(installment.amountDue) - toCents(installment.amountPaid || 0);
  if (outstandingCents <= 0) {
    return 0;
  }

  const flatCents = toCents(rules.lateFeeFlat);
  const pctCents = roundHalfEven(outstandingCents * rules.lateFeePct);
  const capCents = toCents(rules.lateFeeCap);

  let feeCents = Math.max(flatCents, pctCents);
  if (capCents > 0) {
    feeCents = Math.min(feeCents, capCents);
  }
  feeCents = Math.min(feeCents, outstandingCents);

  return fromCents(feeCents);
}

/**
 * Age a loan's installments and return its worst bucket together with the
 * total outstanding and the fees that would be assessed today. The worst
 * bucket drives collections treatment, so a single 60-day miss outranks any
 * number of newer misses.
 */
function ageLoan(loanId, installments, asOfDay) {
  let worstDays = 0;
  let outstandingCents = 0;
  let feeCents = 0;

  for (const inst of installments) {
    const days = daysPastDue(inst, asOfDay);
    if (days === 0) {
      continue;
    }
    worstDays = Math.max(worstDays, days);
    outstandingCents += toCents(inst.amountDue) - toCents(inst.amountPaid || 0);
    feeCents += toCents(lateFee(inst));
  }

  return {
    loanId,
    daysPastDue: worstDays,
    bucket: bucketFor(worstDays),
    outstanding: fromCents(outstandingCents),
    feesDue: fromCents(feeCents),
  };
}

module.exports = { daysPastDue, bucketFor, lateFee, ageLoan, BUCKETS };
