'use strict';

/**
 * Per-key async mutex. Work submitted for the same key runs strictly one at a
 * time in submission order; work for different keys runs concurrently. The
 * chain for a key is dropped once its last task settles so idle keys do not
 * accumulate.
 */
class KeyedMutex {
  constructor() {
    this.tails = new Map();
  }

  /**
   * Run `task` once every previously submitted task for `key` has settled.
   * Resolves or rejects with the task's own outcome. A failing task does not
   * poison the queue for later callers.
   */
  runExclusive(key, task) {
    const previous = this.tails.get(key) || Promise.resolve();
    const run = previous.then(() => task());
    const tail = run.catch(() => undefined);

    this.tails.set(key, tail);
    tail.then(() => {
      if (this.tails.get(key) === tail) {
        this.tails.delete(key);
      }
    });

    return run;
  }

  /**
   * Number of keys with queued or running work. Exposed for health output.
   */
  get size() {
    return this.tails.size;
  }
}

module.exports = { KeyedMutex };
