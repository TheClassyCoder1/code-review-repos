'use strict';

const disputeStore = require('./disputeStore');
const disputeEvents = require('./disputeEvents');
const rules = require('./disputeRules');
const { getOwnedStatement } = require('../services/statementService');
const { assertSafeId } = require('../utils/validate');
const { logger } = require('../utils/logger');

const { STATUS, DisputeRuleError } = rules;

const MAX_DESCRIPTION = 2000;
const MAX_NOTE = 1000;
const MAX_OPEN_PER_STATEMENT = 3;

function notFound() {
  return new DisputeRuleError('no such dispute', 404);
}

function cleanText(value, max, field) {
  if (value === undefined || value === null) {
    return '';
  }
  if (typeof value !== 'string') {
    throw new DisputeRuleError(`${field} must be a string`);
  }
  const trimmed = value.trim();
  if (trimmed.length > max) {
    throw new DisputeRuleError(`${field} must be at most ${max} characters`);
  }
  return trimmed;
}

function round2(value) {
  return Math.round(value * 100) / 100;
}

/**
 * Open a dispute against one of the caller's statements.
 */
function openDispute(ownerId, statementId, input = {}, now = Date.now()) {
  assertSafeId(statementId);
  const statement = getOwnedStatement(statementId, ownerId);
  if (!statement) {
    throw new DisputeRuleError('no such statement', 404);
  }

  if (!rules.isReasonCode(input.reasonCode)) {
    throw new DisputeRuleError(`unsupported reason code: ${input.reasonCode}`);
  }
  if (!rules.withinFilingWindow(statement.period, now)) {
    throw new DisputeRuleError(
      `the filing window for ${statement.period} closed on ${rules.filingDeadline(statement.period)}`,
      422
    );
  }

  const openCount = disputeStore
    .findByStatement(statementId)
    .filter((d) => !rules.isTerminal(d.status)).length;
  if (openCount >= MAX_OPEN_PER_STATEMENT) {
    throw new DisputeRuleError('too many open disputes on this statement', 409);
  }

  const amount = round2(Number(input.amount));
  const description = cleanText(input.description, MAX_DESCRIPTION, 'description');

  const dispute = disputeStore.insert({
    ownerId,
    statementId,
    period: statement.period,
    reasonCode: input.reasonCode,
    amount,
    description,
    reference: typeof input.reference === 'string' ? input.reference.trim() : null,
    status: STATUS.OPEN,
    openedAt: now,
    respondBy: rules.responseDueAt(now),
    notes: [],
    history: [{ at: now, from: null, to: STATUS.OPEN, by: ownerId }],
  });

  disputeEvents.emit(disputeEvents.EVENTS.OPENED, {
    disputeId: dispute.id,
    statementId,
    ownerId,
    status: dispute.status,
  });
  logger.info({ disputeId: dispute.id, statementId, ownerId }, 'dispute.opened');
  return dispute;
}

/**
 * Fetch a dispute the caller owns. Servicing staff use getForStaff instead.
 */
function getOwnedDispute(ownerId, disputeId) {
  assertSafeId(disputeId);
  const dispute = disputeStore.findById(disputeId);
  if (!dispute || dispute.ownerId !== ownerId) {
    throw notFound();
  }
  return dispute;
}

function getForStaff(disputeId) {
  assertSafeId(disputeId);
  const dispute = disputeStore.findById(disputeId);
  if (!dispute) {
    throw notFound();
  }
  return dispute;
}

function listForOwner(ownerId, { status } = {}) {
  let rows = disputeStore.findByOwner(ownerId);
  if (status) {
    rows = rows.filter((d) => d.status === status);
  }
  return rows.sort((a, b) => b.openedAt - a.openedAt);
}

/**
 * The servicing work queue: every non-terminal dispute, oldest first, with an
 * overdue flag so the dashboard can highlight SLA breaches.
 */
function workQueue(now = Date.now()) {
  return disputeStore
    .findAll((d) => !rules.isTerminal(d.status))
    .sort((a, b) => a.openedAt - b.openedAt)
    .map((d) => ({ ...d, overdue: rules.isOverdue(d, now) }));
}

function transition(dispute, to, actorId, extra = {}) {
  rules.assertTransition(dispute.status, to);
  const at = Date.now();
  const updated = disputeStore.update(
    dispute.id,
    { status: to, ...extra },
    { at, from: dispute.status, to, by: actorId }
  );
  disputeEvents.emit(disputeEvents.EVENTS.STATUS_CHANGED, {
    disputeId: updated.id,
    statementId: updated.statementId,
    ownerId: updated.ownerId,
    status: updated.status,
  });
  logger.info({ disputeId: updated.id, from: dispute.status, to, actorId }, 'dispute.transitioned');
  return updated;
}

/** Borrower withdraws their own dispute. */
function withdraw(ownerId, disputeId) {
  const dispute = getOwnedDispute(ownerId, disputeId);
  return transition(dispute, STATUS.WITHDRAWN, ownerId);
}

/** Staff pick up, request info on, or resolve a dispute. */
function staffTransition(staffId, disputeId, to, { creditAmount } = {}) {
  const dispute = getForStaff(disputeId);
  const extra = {};
  if (to === STATUS.RESOLVED_CREDIT) {
    const credit = Number(creditAmount);
    if (!Number.isFinite(credit) || credit <= 0) {
      throw new DisputeRuleError('creditAmount must be a positive number');
    }
    if (credit > dispute.amount) {
      throw new DisputeRuleError('creditAmount cannot exceed the disputed amount');
    }
    extra.creditAmount = round2(credit);
  }
  if (rules.isTerminal(to)) {
    extra.resolvedAt = Date.now();
  }
  return transition(dispute, to, staffId, extra);
}

function addNote(actorId, dispute, text, visibility) {
  const body = cleanText(text, MAX_NOTE, 'note');
  if (!body) {
    throw new DisputeRuleError('note must not be empty');
  }
  const note = { at: Date.now(), by: actorId, visibility, body };
  const updated = disputeStore.update(dispute.id, { notes: [...dispute.notes, note] });
  disputeEvents.emit(disputeEvents.EVENTS.NOTE_ADDED, {
    disputeId: updated.id,
    statementId: updated.statementId,
    ownerId: updated.ownerId,
    status: updated.status,
  });
  return updated;
}

function addCustomerNote(ownerId, disputeId, text) {
  const dispute = getOwnedDispute(ownerId, disputeId);
  if (rules.isTerminal(dispute.status)) {
    throw new DisputeRuleError('dispute is closed', 409);
  }
  const updated = addNote(ownerId, dispute, text, 'customer');
  // A customer reply puts a dispute that was waiting on them back in review.
  if (updated.status === STATUS.AWAITING_CUSTOMER) {
    return transition(updated, STATUS.UNDER_REVIEW, ownerId);
  }
  return updated;
}

function addStaffNote(staffId, disputeId, text, { internal = true } = {}) {
  const dispute = getForStaff(disputeId);
  return addNote(staffId, dispute, text, internal ? 'internal' : 'customer');
}

/**
 * Strip staff-only notes before a dispute is returned to the borrower.
 */
function toCustomerView(dispute) {
  return {
    id: dispute.id,
    statementId: dispute.statementId,
    period: dispute.period,
    reasonCode: dispute.reasonCode,
    reason: rules.describeReason(dispute.reasonCode),
    amount: dispute.amount,
    status: dispute.status,
    openedAt: new Date(dispute.openedAt).toISOString(),
    respondBy: new Date(dispute.respondBy).toISOString(),
    creditAmount: dispute.creditAmount || null,
    notes: dispute.notes.filter((n) => n.visibility === 'customer'),
  };
}

/**
 * Escalate every open dispute past its response SLA. Returns the escalated ids.
 */
function escalateOverdue(now = Date.now()) {
  const escalated = [];
  for (const dispute of workQueue(now)) {
    if (!dispute.overdue || dispute.escalatedAt) {
      continue;
    }
    disputeStore.update(dispute.id, { escalatedAt: now });
    disputeEvents.emit(disputeEvents.EVENTS.ESCALATED, {
      disputeId: dispute.id,
      statementId: dispute.statementId,
      ownerId: dispute.ownerId,
      status: dispute.status,
    });
    escalated.push(dispute.id);
  }
  if (escalated.length) {
    logger.warn({ count: escalated.length }, 'dispute.escalated');
  }
  return escalated;
}

module.exports = {
  openDispute,
  getOwnedDispute,
  getForStaff,
  listForOwner,
  workQueue,
  withdraw,
  staffTransition,
  addCustomerNote,
  addStaffNote,
  toCustomerView,
  escalateOverdue,
  MAX_OPEN_PER_STATEMENT,
};
