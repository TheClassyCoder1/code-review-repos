'use strict';

const test = require('node:test');
const assert = require('node:assert');

const { nextDeliverySlot, isWithinQuietHours, resolveSendTime, HOUR_MS } = require('../../src/part5/delivery/deliveryWindow');
const { normaliseChannelPreferences, selectChannel, ChannelPreferenceError } = require('../../src/part5/delivery/channelSelector');
const { createOutboxStore } = require('../../src/part5/models/outboxStore');
const { DeliveryService, DeliveryConflictError } = require('../../src/part5/services/deliveryService');
const { createDispatcher } = require('../../src/part5/services/dispatcher');
const {
  NOON_MON_2026_10_26,
  STATEMENTS,
  CHANNEL_SETTINGS,
  silentLogger,
  fixedClock,
} = require('./fixtures/deliveryFixtures');

const WINDOW = { startHour: 8, endHour: 20, weekdaysOnly: true };

test('nextDeliverySlot keeps a time already inside the window', () => {
  assert.strictEqual(nextDeliverySlot(NOON_MON_2026_10_26, WINDOW), NOON_MON_2026_10_26);
});

test('nextDeliverySlot moves early morning to window open', () => {
  const early = Date.UTC(2026, 9, 26, 3, 15);
  assert.strictEqual(nextDeliverySlot(early, WINDOW), Date.UTC(2026, 9, 26, 8, 0));
});

test('nextDeliverySlot skips the weekend when weekdaysOnly is set', () => {
  const fridayNight = Date.UTC(2026, 9, 23, 21, 0);
  assert.strictEqual(nextDeliverySlot(fridayNight, WINDOW), Date.UTC(2026, 9, 26, 8, 0));
});

test('isWithinQuietHours handles ranges that wrap midnight', () => {
  const quiet = { startHour: 22, endHour: 7 };
  assert.strictEqual(isWithinQuietHours(Date.UTC(2026, 9, 26, 23, 0), quiet), true);
  assert.strictEqual(isWithinQuietHours(Date.UTC(2026, 9, 26, 6, 59), quiet), true);
  assert.strictEqual(isWithinQuietHours(Date.UTC(2026, 9, 26, 7, 0), quiet), false);
  assert.strictEqual(isWithinQuietHours(Date.UTC(2026, 9, 26, 12, 0), { startHour: 5, endHour: 5 }), false);
});

test('resolveSendTime steps past quiet hours inside the window', () => {
  const quiet = { startHour: 12, endHour: 14 };
  assert.strictEqual(resolveSendTime(NOON_MON_2026_10_26, WINDOW, quiet), NOON_MON_2026_10_26 + 2 * HOUR_MS);
});

test('normaliseChannelPreferences dedupes and defaults', () => {
  assert.deepStrictEqual(normaliseChannelPreferences([' Email', 'portal', 'email']), ['email', 'portal']);
  assert.deepStrictEqual(normaliseChannelPreferences(undefined), ['portal']);
  assert.deepStrictEqual(normaliseChannelPreferences([]), ['portal']);
  assert.throws(() => normaliseChannelPreferences(['fax']), ChannelPreferenceError);
});

test('selectChannel falls through disabled and capped channels', () => {
  assert.strictEqual(selectChannel(['sms', 'email'], CHANNEL_SETTINGS, {}), 'email');
  assert.strictEqual(selectChannel(['email', 'portal'], CHANNEL_SETTINGS, { email: 2 }), 'portal');
  assert.strictEqual(selectChannel(['sms'], CHANNEL_SETTINGS, {}), null);
});

function buildService(clock = fixedClock()) {
  return new DeliveryService({
    outbox: createOutboxStore(),
    channelSettings: CHANNEL_SETTINGS,
    logger: silentLogger(),
    clock,
    window: WINDOW,
  });
}

test('enqueueStatementDelivery rejects a second active job for a statement', () => {
  const service = buildService();
  const job = service.enqueueStatementDelivery(STATEMENTS[0], ['email']);
  assert.strictEqual(job.state, 'pending');
  assert.throws(() => service.enqueueStatementDelivery(STATEMENTS[0], ['email']), DeliveryConflictError);
});

test('claimDueBatch claims each job once', () => {
  const service = buildService();
  service.enqueueStatementDelivery(STATEMENTS[0], ['email']);
  service.enqueueStatementDelivery(STATEMENTS[2], ['portal']);
  const first = service.claimDueBatch(10);
  assert.strictEqual(first.length, 2);
  assert.ok(first.every((j) => j.state === 'in-flight' && j.attempts === 1));
  assert.strictEqual(service.claimDueBatch(10).length, 0);
});

test('markOutcome retries transient failures and dead-letters permanent ones', () => {
  const clock = fixedClock();
  const service = buildService(clock);
  const a = service.enqueueStatementDelivery(STATEMENTS[0], ['email']);
  const b = service.enqueueStatementDelivery(STATEMENTS[2], ['email']);
  service.claimDueBatch(10);

  const retried = service.markOutcome(a.id, { code: 'ECONNRESET' });
  assert.strictEqual(retried.state, 'pending');
  assert.ok(retried.notBefore >= clock());

  const dead = service.markOutcome(b.id, { response: { status: 400 } });
  assert.strictEqual(dead.state, 'dead');
  assert.strictEqual(service.markOutcome(b.id, null), null);
});

test('dispatcher delivers due jobs through the channel transport', async () => {
  const service = buildService();
  service.enqueueStatementDelivery(STATEMENTS[0], ['email']);
  const sent = [];
  const dispatcher = createDispatcher({
    deliveryService: service,
    transports: { email: { send: async (msg) => sent.push(msg) } },
    logger: silentLogger(),
    batchSize: 5,
    intervalMs: 1000,
  });
  const counts = await dispatcher.runOnce();
  assert.deepStrictEqual(counts, { claimed: 1, delivered: 1, retried: 0, dead: 0 });
  assert.deepStrictEqual(Object.keys(sent[0]).sort(), ['jobId', 'ownerId', 'statementId']);
});
