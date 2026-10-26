/**
 * In-process mutual exclusion keyed by an arbitrary string.
 *
 * Callers for the same key run strictly one after another in arrival order;
 * different keys never block each other. The tail entry is dropped once the
 * last waiter for a key finishes, so idle keys do not accumulate.
 */
export class KeyedMutex {
  private readonly tails = new Map<string, Promise<void>>();

  async runExclusive<T>(key: string, task: () => Promise<T>): Promise<T> {
    const previous = this.tails.get(key) ?? Promise.resolve();
    let release!: () => void;
    const current = new Promise<void>((resolve) => {
      release = resolve;
    });
    const tail = previous.then(() => current);
    this.tails.set(key, tail);

    await previous;
    try {
      return await task();
    } finally {
      release();
      if (this.tails.get(key) === tail) {
        this.tails.delete(key);
      }
    }
  }

  isLocked(key: string): boolean {
    return this.tails.has(key);
  }
}
