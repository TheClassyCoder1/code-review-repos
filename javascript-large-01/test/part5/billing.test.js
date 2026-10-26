'use strict';

const test = require('node:test');
const assert = require('node:assert');

const { computeLateFee, allocatePayment, formatCents } = require('../../src/part5/billing/lateFeeCalculator');
const { daysPastDue, buildReminderSchedule, reminderTemplateFor } = require('../../src/part5/reminders/reminderCadence');
const { summariseTransactions, groupByPeriod } = require('../../src/part5/summary/statementSummary');
const { escapeCsvField, toCsv, LINE_ITEM_COLUMNS } = require('../../src/part5/export/statementCsv');
const { computeBackoffDelay, classifyDeliveryError } = require('../../src/part5/retry/backoffPolicy');
const { parsePageParams, paginateById, encodeCursor } = require('../../src/part5/http/pagination');
const { ValidationError } = require('../../src/utils/validate');

const DAY = 24 * 60 * 60 * 1000;

test('computeLateFee respects grace, floor and cap', () => {
  assert.strictEqual(computeLateFee(100000, 5), 0);
  assert.strictEqual(computeLateFee(100000, 6), 2500);
  assert.strictEqual(computeLateFee(200000, 6), 3000);
  assert.strictEqual(computeLateFee(1000000, 30), 4000);
  assert.strictEqual(computeLateFee(1200, 30), 1200);
  assert.strictEqual(computeLateFee(0, 30), 0);
});

test('allocatePayment applies the waterfall and reports overpayment', () => {
  const result = allocatePayment(5000, { fees: 1000, interest: 1500, principal: 10000 });
  assert.deepStrictEqual(result.applied, { fees: 1000, interest: 1500, principal: 2500 });
  assert.strictEqual(result.settled, false);

  const over = allocatePayment(3000, { fees: 0, interest: 500, principal: 2000 });
  assert.strictEqual(over.overpaymentCents, 500);
  assert.strictEqual(over.settled, true);
});

test('formatCents renders two decimal places', () => {
  assert.strictEqual(formatCents(124055), '1240.55');
  assert.strictEqual(formatCents(-5), '-0.05');
});

test('reminder schedule drops past reminders and sorts offsets', () => {
  const due = Date.UTC(2026, 10, 10);
  const now = due - 3 * DAY;
  const schedule = buildReminderSchedule(due, now, [10, -5, -1, -1, 3]);
  assert.deepStrictEqual(schedule.map((r) => r.offsetDays), [-1, 3, 10]);
  assert.strictEqual(schedule[0].kind, 'upcoming');
  assert.strictEqual(daysPastDue(due, due + 2.5 * DAY), 2);
  assert.strictEqual(reminderTemplateFor(due, due + 20 * DAY), 'reminder-collections-handoff');
});

test('summariseTransactions flags unknown types', () => {
  const summary = summariseTransactions(10000, [
    { id: 't1', type: 'interest', amountCents: 250 },
    { id: 't2', type: 'payment', amountCents: 5000 },
    { id: 't3', type: 'mystery', amountCents: 10 },
  ]);
  assert.strictEqual(summary.closingBalanceCents, 5250);
  assert.deepStrictEqual(summary.unclassified, ['t3']);
  assert.strictEqual(summary.needsReview, true);
});

test('groupByPeriod orders periods and counts delivery state', () => {
  const { periods, skipped } = groupByPeriod([
    { period: '2026-10', deliveredAt: 1 },
    { period: '2026-09' },
    { period: '2026-10' },
    { period: 'bogus' },
  ]);
  assert.deepStrictEqual(periods.map((p) => p.period), ['2026-09', '2026-10']);
  assert.deepStrictEqual(periods[1], { period: '2026-10', total: 2, delivered: 1, pending: 1 });
  assert.strictEqual(skipped, 1);
});

test('CSV escaping quotes separators and neutralises formulas', () => {
  assert.strictEqual(escapeCsvField('a,b'), '"a,b"');
  assert.strictEqual(escapeCsvField('say "hi"'), '"say ""hi"""');
  assert.strictEqual(escapeCsvField('=SUM(A1)'), "'=SUM(A1)");
  assert.strictEqual(escapeCsvField(-12.5), '-12.5');
  const csv = toCsv([{ postedOn: '2026-10-01', reference: 'R1', description: 'Fee', amountCents: 2500, secret: 'x' }], LINE_ITEM_COLUMNS);
  assert.strictEqual(csv, 'Posted,Reference,Description,Amount\r\n2026-10-01,R1,Fee,25\r\n');
});

test('backoff grows, caps and stops', () => {
  const policy = { baseDelayMs: 100, maxDelayMs: 1000, maxAttempts: 5 };
  const almostOne = () => 0.999999;
  assert.strictEqual(computeBackoffDelay(1, policy, almostOne), 99);
  assert.strictEqual(computeBackoffDelay(5, policy, almostOne), 999);
  assert.strictEqual(computeBackoffDelay(6, policy, almostOne), null);
  assert.deepStrictEqual(classifyDeliveryError({ response: { status: 503, headers: { 'retry-after': '2' } } }), {
    retryable: true,
    reason: 'http:503',
    retryAfterMs: 2000,
  });
  assert.strictEqual(classifyDeliveryError({ response: { status: 404 } }).retryable, false);
});

test('pagination clamps limits and walks pages by cursor', () => {
  assert.deepStrictEqual(parsePageParams({}), { limit: 25, after: null });
  assert.strictEqual(parsePageParams({ limit: '500' }).limit, 100);
  assert.throws(() => parsePageParams({ limit: '0' }), ValidationError);
  assert.throws(() => parsePageParams({ cursor: 'not-a-cursor' }), ValidationError);

  const rows = ['d', 'a', 'c', 'b', 'e'].map((id) => ({ id }));
  const first = paginateById(rows, { limit: 2, after: null });
  assert.deepStrictEqual(first.items.map((r) => r.id), ['a', 'b']);
  assert.strictEqual(first.nextCursor, encodeCursor('b'));
  const last = paginateById(rows, parsePageParams({ limit: '3', cursor: first.nextCursor }));
  assert.deepStrictEqual(last.items.map((r) => r.id), ['c', 'd', 'e']);
  assert.strictEqual(last.nextCursor, null);
});
