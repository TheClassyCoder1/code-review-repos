'use strict';

/**
 * In-process keyed mutex. Work submitted for the same key runs strictly one at
 * a time in submission order; work for different keys runs concurrently.
 *
 * The service runs as a single instance per shard (loans are routed to a shard
 * by id at the gateway), so an in-process lock is sufficient for per-loan
 * serialisation.
 */
class KeyedLock {
  constructor() {
    this.tails = new Map();
  }

  /**
   * Run fn while holding the lock for key. Resolves or rejects with fn's
   * outcome; a failure in one holder never blocks the next one.
   */
  run(key, fn) {
    const previous = this.tails.get(key) || Promise.resolve();
    const current = previous.then(() => fn());
    const tail = current.catch(() => undefined);
    this.tails.set(key, tail);
    tail.then(() => {
      if (this.tails.get(key) === tail) {
        this.tails.delete(key);
      }
    });
    return current;
  }

  /** Number of keys that currently have queued or running work. */
  get size() {
    return this.tails.size;
  }
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function round2(value) {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

module.exports = { KeyedLock, sleep, round2 };
