'use strict';

const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const os = require('os');
const path = require('path');

const { roundHalfEven, splitEvenly, toCents } = require('../../src/part4/money');
const { nextBusinessDay, addMonths } = require('../../src/part4/businessDays');
const { daysPastDue, bucketFor, lateFee, ageLoan } = require('../../src/part4/delinquency');
const { buildSchedule } = require('../../src/part4/paymentPlanService');
const planStore = require('../../src/part4/paymentPlanStore');
const coordinator = require('../../src/part4/paymentPlanCoordinator');
const { KeyedMutex } = require('../../src/part4/keyedMutex');
const { formatCsvRow } = require('../../src/part4/spoolFile');
const { runDelinquencyExport } = require('../../src/part4/delinquencyBatchJob');
const { stageReceipts, dedupePayments } = require('../../src/part4/receiptBatch');
const { retryWithBackoff } = require('../../src/part4/retry');
const { payoffQuote, nextInstallmentToPay } = require('../../src/part4/payoffQuote');
const { assessHardshipEligibility } = require('../../src/part4/hardship');

test('roundHalfEven rounds exact halves to the even neighbour', () => {
  assert.strictEqual(roundHalfEven(2.5), 2);
  assert.strictEqual(roundHalfEven(3.5), 4);
  assert.strictEqual(roundHalfEven(3.49), 3);
});

test('splitEvenly sums exactly to the total', () => {
  const parts = splitEvenly(1001, 3);
  assert.deepStrictEqual(parts, [334, 334, 333]);
  assert.strictEqual(parts.reduce((a, b) => a + b, 0), 1001);
});

test('due dates roll over weekends and month ends clamp', () => {
  assert.strictEqual(nextBusinessDay('2026-10-24'), '2026-10-26');
  assert.strictEqual(addMonths('2026-01-31', 1), '2026-02-28');
});

test('delinquency respects grace period and buckets', () => {
  const inst = { loanId: 'loan-1', dueDate: '2026-09-01', amountDue: 200, amountPaid: 0 };
  assert.strictEqual(daysPastDue(inst, '2026-09-04'), 0);
  assert.strictEqual(daysPastDue(inst, '2026-10-01'), 30);
  assert.strictEqual(bucketFor(30), '30-59');
  assert.strictEqual(lateFee(inst), 25);
  const aged = ageLoan('loan-1', [inst], '2026-12-15');
  assert.strictEqual(aged.bucket, '90+');
});

test('schedule principal sums to the balance', () => {
  const schedule = buildSchedule(1000, 3, '2026-11-02');
  const principal = schedule.reduce((sum, s) => sum + toCents(s.principal), 0);
  assert.strictEqual(principal, 100000);
  assert.strictEqual(schedule.length, 3);
});

test('concurrent plan requests for one loan create a single plan', async () => {
  planStore._reset();
  const request = { loanId: 'loan-77', ownerId: 'user-7', balance: 600, installments: 3, firstDueDay: '2026-11-02' };
  const slow = { check: () => new Promise((r) => setTimeout(() => r({ affordable: true }), 10)) };
  const [a, b] = await Promise.all([coordinator.requestPlan(request, slow), coordinator.requestPlan(request, slow)]);
  assert.strictEqual(a.plan.id, b.plan.id);
  assert.deepStrictEqual([a.created, b.created].sort(), [false, true]);
});

test('keyed mutex keeps going after a failed task', async () => {
  const mutex = new KeyedMutex();
  await assert.rejects(mutex.runExclusive('k', async () => { throw new Error('boom'); }));
  assert.strictEqual(await mutex.runExclusive('k', async () => 42), 42);
});

test('csv rows neutralise formula prefixes', () => {
  const line = formatCsvRow({ loanId: '=cmd', bucket: '30-59', daysPastDue: 31, outstanding: 10, feesDue: 2.5 });
  assert.strictEqual(line, "'=cmd,30-59,31,10.00,2.50\r\n");
});

test('delinquency export writes a spool file and stops cleanly', async () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'spool-test-'));
  const rows = [
    { loanId: 'loan-1', dueDate: '2026-08-01', amountDue: 100, amountPaid: 0 },
    { loanId: 'loan-2', dueDate: '2026-10-20', amountDue: 100, amountPaid: 100 },
  ];
  const summary = await runDelinquencyExport(rows, '2026-10-26', { spoolDir: dir, exportId: 'exp-1', heartbeatMs: 5 });
  assert.strictEqual(summary.delinquent, 1);
  const body = fs.readFileSync(summary.filePath, 'utf8');
  assert.match(body, /^loanId,bucket/);
});

test('receipt batches dedupe and publish', async () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'receipt-test-'));
  const payments = [
    { paymentId: 'pay-1', loanId: 'loan-1', postedAt: '2026-10-01T10:00:00Z', amount: 50, appliedToPrincipal: 50 },
    { paymentId: 'pay-1', loanId: 'loan-1', postedAt: '2026-10-02T10:00:00Z', amount: 60, appliedToPrincipal: 60 },
  ];
  assert.strictEqual(dedupePayments(payments).unique.length, 1);
  const result = await stageReceipts('batch-1', payments, { stagingRoot: root, tmpRoot: root });
  assert.strictEqual(result.written, 1);
  const doc = JSON.parse(fs.readFileSync(path.join(result.directory, 'pay-1.json'), 'utf8'));
  assert.strictEqual(doc.amount, 60);
});

test('retry stops on non-retryable errors', async () => {
  let calls = 0;
  await assert.rejects(
    retryWithBackoff(async () => { calls += 1; throw new Error('nope'); }, { attempts: 4, isRetryable: () => false }),
    (err) => err.attempts === 1
  );
  assert.strictEqual(calls, 1);
});

test('payoff and next installment', () => {
  const plan = {
    id: 'plan-000001',
    startDay: '2026-10-01',
    schedule: [
      { sequence: 1, dueDate: '2026-10-01', principal: 100, amountDue: 101, paid: true },
      { sequence: 2, dueDate: '2026-11-02', principal: 100, amountDue: 101, paid: false },
    ],
  };
  const quote = payoffQuote(plan, '2026-10-31', 365);
  assert.strictEqual(quote.principal, 100);
  assert.strictEqual(quote.interest, 0.3);
  assert.strictEqual(nextInstallmentToPay(plan, '2026-10-26').sequence, 2);
});

test('hardship rejects loans with an active plan', () => {
  const verdict = assessHardshipEligibility({ daysPastDue: 40 }, [{ status: 'active' }]);
  assert.strictEqual(verdict.eligible, false);
});
