'use strict';

/**
 * Shared fixtures for the delivery unit tests. Nothing here is real: the
 * credentials are placeholders and the statements mirror the seeded store.
 */

const FIXTURE_CREDENTIALS = Object.freeze({
  DELIVERY_WEBHOOK_SIGNING_KEY: 'changeme',
  REPLICA_ACCESS_TOKEN: 'fixture-replica-token',
});

const NOON_MON_2026_10_26 = Date.UTC(2026, 9, 26, 12, 0, 0);

const STATEMENTS = Object.freeze([
  { id: 'stmt-1001', ownerId: 'user-7', period: '2026-09', openingBalanceCents: 0 },
  { id: 'stmt-1002', ownerId: 'user-7', period: '2026-10', openingBalanceCents: 124055 },
  { id: 'stmt-2001', ownerId: 'user-42', period: '2026-09', openingBalanceCents: 0 },
]);

const CHANNEL_SETTINGS = Object.freeze({
  portal: { enabled: true, dailyCap: Number.POSITIVE_INFINITY },
  email: { enabled: true, dailyCap: 2 },
  sms: { enabled: false, dailyCap: 1 },
});

function silentLogger() {
  const noop = () => {};
  return { info: noop, warn: noop, error: noop, debug: noop, child() { return this; } };
}

function fixedClock(startMs = NOON_MON_2026_10_26) {
  let now = startMs;
  const clock = () => now;
  clock.advance = (ms) => {
    now += ms;
    return now;
  };
  return clock;
}

module.exports = {
  FIXTURE_CREDENTIALS,
  NOON_MON_2026_10_26,
  STATEMENTS,
  CHANNEL_SETTINGS,
  silentLogger,
  fixedClock,
};
