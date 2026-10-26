'use strict';

/**
 * Business rules for statement disputes: the supported reason codes, the
 * filing window, and the status machine a dispute moves through. Kept free of
 * I/O so the rules can be unit tested on their own.
 */

const REASON_CODES = Object.freeze({
  DUPLICATE_CHARGE: 'Charge appears more than once on the statement',
  INCORRECT_AMOUNT: 'Charged amount differs from the agreed amount',
  UNRECOGNISED_FEE: 'Fee the borrower does not recognise',
  PAYMENT_NOT_APPLIED: 'A payment was made but not credited',
  INTEREST_MISCALCULATED: 'Interest for the period looks wrong',
});

/** Days after the statement period closes during which a dispute may be filed. */
const FILING_WINDOW_DAYS = 60;

/** Days the servicing team has to respond before a dispute is escalated. */
const RESPONSE_SLA_DAYS = 30;

const STATUS = Object.freeze({
  OPEN: 'OPEN',
  UNDER_REVIEW: 'UNDER_REVIEW',
  AWAITING_CUSTOMER: 'AWAITING_CUSTOMER',
  RESOLVED_CREDIT: 'RESOLVED_CREDIT',
  RESOLVED_NO_CHANGE: 'RESOLVED_NO_CHANGE',
  WITHDRAWN: 'WITHDRAWN',
});

const TRANSITIONS = Object.freeze({
  [STATUS.OPEN]: [STATUS.UNDER_REVIEW, STATUS.WITHDRAWN],
  [STATUS.UNDER_REVIEW]: [STATUS.AWAITING_CUSTOMER, STATUS.RESOLVED_CREDIT, STATUS.RESOLVED_NO_CHANGE],
  [STATUS.AWAITING_CUSTOMER]: [STATUS.UNDER_REVIEW, STATUS.WITHDRAWN],
  [STATUS.RESOLVED_CREDIT]: [],
  [STATUS.RESOLVED_NO_CHANGE]: [],
  [STATUS.WITHDRAWN]: [],
});

const TERMINAL = new Set([STATUS.RESOLVED_CREDIT, STATUS.RESOLVED_NO_CHANGE, STATUS.WITHDRAWN]);

const DAY_MS = 24 * 60 * 60 * 1000;

class DisputeRuleError extends Error {
  constructor(message, status = 400) {
    super(message);
    this.name = 'DisputeRuleError';
    this.status = status;
  }
}

function isReasonCode(code) {
  return typeof code === 'string' && Object.prototype.hasOwnProperty.call(REASON_CODES, code);
}

function describeReason(code) {
  return isReasonCode(code) ? REASON_CODES[code] : null;
}

/**
 * The last instant of a YYYY-MM statement period, in UTC.
 */
function periodEnd(period) {
  const match = /^(\d{4})-(\d{2})$/.exec(String(period));
  if (!match) {
    throw new DisputeRuleError(`invalid statement period: ${period}`);
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  if (month < 1 || month > 12) {
    throw new DisputeRuleError(`invalid statement period: ${period}`);
  }
  // Day 0 of the following month is the last day of this one.
  return Date.UTC(year, month, 0, 23, 59, 59, 999);
}

/**
 * True when a dispute against the given period can still be filed at `now`.
 */
function withinFilingWindow(period, now = Date.now()) {
  const closesAt = periodEnd(period) + FILING_WINDOW_DAYS * DAY_MS;
  return now <= closesAt;
}

function filingDeadline(period) {
  return new Date(periodEnd(period) + FILING_WINDOW_DAYS * DAY_MS).toISOString();
}

function responseDueAt(openedAt) {
  return openedAt + RESPONSE_SLA_DAYS * DAY_MS;
}

function isOverdue(dispute, now = Date.now()) {
  if (TERMINAL.has(dispute.status)) {
    return false;
  }
  return now > responseDueAt(dispute.openedAt);
}

function canTransition(from, to) {
  const allowed = TRANSITIONS[from];
  return Array.isArray(allowed) && allowed.includes(to);
}

function assertTransition(from, to) {
  if (!Object.prototype.hasOwnProperty.call(TRANSITIONS, to)) {
    throw new DisputeRuleError(`unknown dispute status: ${to}`);
  }
  if (!canTransition(from, to)) {
    throw new DisputeRuleError(`cannot move dispute from ${from} to ${to}`, 409);
  }
}

function isTerminal(status) {
  return TERMINAL.has(status);
}

module.exports = {
  REASON_CODES,
  FILING_WINDOW_DAYS,
  RESPONSE_SLA_DAYS,
  STATUS,
  TRANSITIONS,
  DisputeRuleError,
  isReasonCode,
  describeReason,
  periodEnd,
  withinFilingWindow,
  filingDeadline,
  responseDueAt,
  isOverdue,
  canTransition,
  assertTransition,
  isTerminal,
};
