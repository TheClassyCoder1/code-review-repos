'use strict';

const { setImmediate: nextTick } = require('timers/promises');

/**
 * Minimal async key/value store standing in for the document datastore used by
 * the repayments features. Every operation yields to the event loop first, the
 * same way a real network round-trip would, and values are deep-copied in and
 * out so callers never share references with the store.
 */
class AsyncKv {
  constructor(name) {
    this.name = name;
    this.rows = new Map();
  }

  async get(key) {
    await nextTick();
    const value = this.rows.get(key);
    return value === undefined ? null : structuredClone(value);
  }

  async has(key) {
    await nextTick();
    return this.rows.has(key);
  }

  async set(key, value) {
    await nextTick();
    this.rows.set(key, structuredClone(value));
    return value;
  }

  async delete(key) {
    await nextTick();
    return this.rows.delete(key);
  }

  /**
   * Insert only when the key is absent. Resolves true when the value was
   * written, false when another writer got there first.
   */
  async setIfAbsent(key, value) {
    await nextTick();
    if (this.rows.has(key)) {
      return false;
    }
    this.rows.set(key, structuredClone(value));
    return true;
  }

  async keys() {
    await nextTick();
    return [...this.rows.keys()];
  }

  /** Synchronous seeding helper for fixtures and tests. */
  seed(key, value) {
    this.rows.set(key, structuredClone(value));
  }

  clear() {
    this.rows.clear();
  }
}

module.exports = { AsyncKv };
