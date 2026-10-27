'use strict';

/**
 * Money helpers. Amounts are handled as integer minor units (cents) internally
 * so installment splits and fee calculations never accumulate floating point
 * drift; conversion to and from decimal happens only at the edges.
 */

class MoneyError extends Error {
  constructor(message) {
    super(message);
    this.name = 'MoneyError';
    this.status = 400;
  }
}

/**
 * Convert a decimal amount (e.g. 12.34) to integer cents, rounding half away
 * from zero. Rejects non-finite input.
 */
function toCents(amount) {
  const n = Number(amount);
  if (!Number.isFinite(n)) {
    throw new MoneyError(`amount is not a finite number: ${amount}`);
  }
  const sign = n < 0 ? -1 : 1;
  return sign * Math.round(Math.abs(n) * 100);
}

/**
 * Convert integer cents back to a decimal number with two places.
 */
function fromCents(cents) {
  return Number((cents / 100).toFixed(2));
}

/**
 * Banker's rounding (round half to even) of a fractional cent value to an
 * integer number of cents. Used for interest accrual so that, across many
 * installments, rounding does not systematically favour lender or borrower.
 */
function roundHalfEven(value) {
  if (!Number.isFinite(value)) {
    throw new MoneyError('cannot round a non-finite value');
  }
  const floor = Math.floor(value);
  const diff = value - floor;
  const epsilon = 1e-9;

  if (diff > 0.5 + epsilon) {
    return floor + 1;
  }
  if (diff < 0.5 - epsilon) {
    return floor;
  }
  // Exactly half: round to the even neighbour.
  return floor % 2 === 0 ? floor : floor + 1;
}

/**
 * Split a total (in cents) into `parts` integer amounts that sum exactly to the
 * total. Remainder cents are distributed one at a time to the earliest parts,
 * so the first installments may be one cent larger than later ones.
 */
function splitEvenly(totalCents, parts) {
  if (!Number.isInteger(totalCents) || totalCents < 0) {
    throw new MoneyError('total must be a non-negative integer number of cents');
  }
  if (!Number.isInteger(parts) || parts <= 0) {
    throw new MoneyError('parts must be a positive integer');
  }

  const base = Math.floor(totalCents / parts);
  let remainder = totalCents - base * parts;
  const result = new Array(parts);

  for (let i = 0; i < parts; i += 1) {
    if (remainder > 0) {
      result[i] = base + 1;
      remainder -= 1;
    } else {
      result[i] = base;
    }
  }
  return result;
}

module.exports = { toCents, fromCents, roundHalfEven, splitEvenly, MoneyError };
