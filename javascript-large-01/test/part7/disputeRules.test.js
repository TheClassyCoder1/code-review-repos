'use strict';

const test = require('node:test');
const assert = require('node:assert');
const rules = require('../../src/part7/disputeRules');

const DAY = 24 * 60 * 60 * 1000;

test('reason codes are recognised and described', () => {
  assert.strictEqual(rules.isReasonCode('DUPLICATE_CHARGE'), true);
  assert.strictEqual(rules.isReasonCode('duplicate_charge'), false);
  assert.strictEqual(rules.isReasonCode('toString'), false);
  assert.strictEqual(rules.isReasonCode(undefined), false);
  assert.match(rules.describeReason('UNRECOGNISED_FEE'), /Fee/);
  assert.strictEqual(rules.describeReason('NOPE'), null);
});

test('periodEnd is the last millisecond of the month in UTC', () => {
  assert.strictEqual(new Date(rules.periodEnd('2026-02')).toISOString(), '2026-02-28T23:59:59.999Z');
  assert.strictEqual(new Date(rules.periodEnd('2028-02')).toISOString(), '2028-02-29T23:59:59.999Z');
  assert.strictEqual(new Date(rules.periodEnd('2026-12')).toISOString(), '2026-12-31T23:59:59.999Z');
});

test('periodEnd rejects malformed periods', () => {
  assert.throws(() => rules.periodEnd('2026-13'), rules.DisputeRuleError);
  assert.throws(() => rules.periodEnd('2026-1'), rules.DisputeRuleError);
  assert.throws(() => rules.periodEnd('not-a-period'), rules.DisputeRuleError);
});

test('filing window closes 60 days after the period', () => {
  const end = rules.periodEnd('2026-09');
  assert.strictEqual(rules.withinFilingWindow('2026-09', end + 59 * DAY), true);
  assert.strictEqual(rules.withinFilingWindow('2026-09', end + 60 * DAY), true);
  assert.strictEqual(rules.withinFilingWindow('2026-09', end + 60 * DAY + 1), false);
  assert.strictEqual(rules.filingDeadline('2026-09'), new Date(end + 60 * DAY).toISOString());
});

test('status machine allows only forward transitions', () => {
  const { STATUS } = rules;
  assert.strictEqual(rules.canTransition(STATUS.OPEN, STATUS.UNDER_REVIEW), true);
  assert.strictEqual(rules.canTransition(STATUS.OPEN, STATUS.RESOLVED_CREDIT), false);
  assert.strictEqual(rules.canTransition(STATUS.RESOLVED_CREDIT, STATUS.OPEN), false);
  assert.throws(() => rules.assertTransition(STATUS.OPEN, 'BOGUS'), /unknown dispute status/);
  assert.throws(
    () => rules.assertTransition(STATUS.WITHDRAWN, STATUS.UNDER_REVIEW),
    (err) => err.status === 409
  );
});

test('terminal disputes are never overdue', () => {
  const openedAt = Date.UTC(2026, 0, 1);
  const later = openedAt + 45 * DAY;
  assert.strictEqual(rules.isOverdue({ status: rules.STATUS.OPEN, openedAt }, later), true);
  assert.strictEqual(rules.isOverdue({ status: rules.STATUS.WITHDRAWN, openedAt }, later), false);
  assert.strictEqual(rules.isOverdue({ status: rules.STATUS.OPEN, openedAt }, openedAt + DAY), false);
});
