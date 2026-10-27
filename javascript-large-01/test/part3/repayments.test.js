'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { KeyedLock, round2 } = require('../../src/part3/utils/keyedLock');
const { buildSchedule, minimumViablePayment, addMonths } = require('../../src/part3/services/scheduleBuilder');
const { AsyncKv } = require('../../src/part3/stores/asyncKv');

test('keyed lock serialises work for the same key', async () => {
  const lock = new KeyedLock();
  const order = [];
  const slow = lock.run('loan-1', async () => {
    await new Promise((r) => setTimeout(r, 20));
    order.push('first');
  });
  const fast = lock.run('loan-1', async () => {
    order.push('second');
  });
  await Promise.all([slow, fast]);
  assert.deepStrictEqual(order, ['first', 'second']);
});

test('keyed lock keeps running after a failed holder', async () => {
  const lock = new KeyedLock();
  await assert.rejects(lock.run('k', async () => { throw new Error('boom'); }));
  const value = await lock.run('k', async () => 42);
  assert.strictEqual(value, 42);
});

test('schedule clears the balance and ends on a short final payment', () => {
  const schedule = buildSchedule({ principal: 1000, monthlyRate: 0.01, payment: 100, firstDueDate: '2026-11-05' });
  const last = schedule.rows[schedule.rows.length - 1];
  assert.strictEqual(last.balance, 0);
  assert.ok(last.payment <= 100);
  assert.strictEqual(schedule.periods, 11);
});

test('minimum viable payment exceeds the first month interest', () => {
  assert.strictEqual(minimumViablePayment(1000, 0.01), 11);
});

test('addMonths clamps to the end of shorter months', () => {
  assert.strictEqual(addMonths('2026-01-31', 1), '2026-02-28');
  assert.strictEqual(addMonths('2026-01-31', 2), '2026-03-31');
});

test('round2 rounds half up on cents', () => {
  assert.strictEqual(round2(1.005), 1.01);
});

test('async kv returns copies, not shared references', async () => {
  const kv = new AsyncKv('t');
  await kv.set('a', { n: 1 });
  const first = await kv.get('a');
  first.n = 99;
  assert.strictEqual((await kv.get('a')).n, 1);
  assert.strictEqual(await kv.setIfAbsent('a', { n: 2 }), false);
});
