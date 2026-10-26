'use strict';

const test = require('node:test');
const assert = require('node:assert');
const disputeService = require('../../src/part7/disputeService');
const disputeStore = require('../../src/part7/disputeStore');
const disputeEvents = require('../../src/part7/disputeEvents');
const rules = require('../../src/part7/disputeRules');

const { STATUS } = rules;
const DAY = 24 * 60 * 60 * 1000;
// Inside the filing window for the seeded 2026-10 statements.
const NOW = rules.periodEnd('2026-10') + 5 * DAY;

function freshOpen(overrides = {}) {
  return disputeService.openDispute(
    'user-7',
    'stmt-1002',
    { reasonCode: 'DUPLICATE_CHARGE', amount: 42.5, description: 'charged twice', ...overrides },
    NOW
  );
}

test.beforeEach(() => {
  disputeStore.clear();
  disputeEvents.reset();
});

test('opens a dispute on an owned statement and emits an event', () => {
  const seen = [];
  disputeEvents.on(disputeEvents.EVENTS.OPENED, (e) => seen.push(e));
  const dispute = freshOpen();
  assert.strictEqual(dispute.status, STATUS.OPEN);
  assert.strictEqual(dispute.period, '2026-10');
  assert.strictEqual(dispute.respondBy, NOW + rules.RESPONSE_SLA_DAYS * DAY);
  assert.strictEqual(seen.length, 1);
  assert.strictEqual(seen[0].disputeId, dispute.id);
  assert.deepStrictEqual(Object.keys(seen[0]).sort(), [
    'disputeId',
    'occurredAt',
    'ownerId',
    'statementId',
    'status',
    'type',
  ]);
});

test('refuses statements owned by someone else', () => {
  assert.throws(
    () => disputeService.openDispute('user-7', 'stmt-2001', { reasonCode: 'DUPLICATE_CHARGE' }, NOW),
    (err) => err.status === 404
  );
});

test('refuses unknown reason codes', () => {
  assert.throws(() => freshOpen({ reasonCode: 'BECAUSE' }), /unsupported reason code/);
});

test('refuses disputes after the filing window', () => {
  const late = rules.periodEnd('2026-09') + 61 * DAY;
  assert.throws(
    () => disputeService.openDispute('user-7', 'stmt-1001', { reasonCode: 'DUPLICATE_CHARGE', amount: 1 }, late),
    (err) => err.status === 422
  );
});

test('caps open disputes per statement', () => {
  for (let i = 0; i < disputeService.MAX_OPEN_PER_STATEMENT; i += 1) {
    freshOpen();
  }
  assert.throws(() => freshOpen(), (err) => err.status === 409);
});

test('withdrawn disputes no longer count toward the cap', () => {
  const first = freshOpen();
  freshOpen();
  freshOpen();
  disputeService.withdraw('user-7', first.id);
  assert.doesNotThrow(() => freshOpen());
});

test('staff can review and resolve with a credit', () => {
  const dispute = freshOpen();
  disputeService.staffTransition('staff-1', dispute.id, STATUS.UNDER_REVIEW);
  const resolved = disputeService.staffTransition('staff-1', dispute.id, STATUS.RESOLVED_CREDIT, {
    creditAmount: '20.005',
  });
  assert.strictEqual(resolved.status, STATUS.RESOLVED_CREDIT);
  assert.strictEqual(resolved.creditAmount, 20.01);
  assert.ok(resolved.resolvedAt);
  assert.deepStrictEqual(
    resolved.history.map((h) => h.to),
    [STATUS.OPEN, STATUS.UNDER_REVIEW, STATUS.RESOLVED_CREDIT]
  );
});

test('resolution credit must be positive', () => {
  const dispute = freshOpen();
  disputeService.staffTransition('staff-1', dispute.id, STATUS.UNDER_REVIEW);
  assert.throws(
    () => disputeService.staffTransition('staff-1', dispute.id, STATUS.RESOLVED_CREDIT, { creditAmount: 0 }),
    /creditAmount must be a positive number/
  );
});

test('a customer reply moves an awaiting dispute back into review', () => {
  const dispute = freshOpen();
  disputeService.staffTransition('staff-1', dispute.id, STATUS.UNDER_REVIEW);
  disputeService.staffTransition('staff-1', dispute.id, STATUS.AWAITING_CUSTOMER);
  const updated = disputeService.addCustomerNote('user-7', dispute.id, 'receipt attached');
  assert.strictEqual(updated.status, STATUS.UNDER_REVIEW);
  assert.strictEqual(updated.notes.length, 1);
});

test('customer view hides internal notes', () => {
  const dispute = freshOpen();
  disputeService.addStaffNote('staff-1', dispute.id, 'checked ledger, looks real');
  disputeService.addStaffNote('staff-1', dispute.id, 'we are looking into it', { internal: false });
  const view = disputeService.toCustomerView(disputeService.getOwnedDispute('user-7', dispute.id));
  assert.strictEqual(view.notes.length, 1);
  assert.strictEqual(view.notes[0].body, 'we are looking into it');
});

test('other borrowers cannot read a dispute', () => {
  const dispute = freshOpen();
  assert.throws(() => disputeService.getOwnedDispute('user-42', dispute.id), (err) => err.status === 404);
});

test('escalates overdue disputes exactly once', () => {
  const dispute = freshOpen();
  const later = NOW + (rules.RESPONSE_SLA_DAYS + 1) * DAY;
  assert.deepStrictEqual(disputeService.escalateOverdue(later), [dispute.id]);
  assert.deepStrictEqual(disputeService.escalateOverdue(later), []);
});
