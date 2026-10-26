/**
 * Map with a fixed capacity: inserting past the limit evicts the oldest
 * entries. It is a drop-in replacement for Map, so consumers that only need
 * get/set/has can be handed a BoundedMap without knowing about the size limit.
 */
export class BoundedMap<K, V> extends Map<K, V> {
  constructor(private readonly capacity: number) {
    super();
    if (!Number.isInteger(capacity) || capacity < 1) {
      throw new RangeError(`BoundedMap capacity must be a positive integer, got ${capacity}`);
    }
  }

  override set(key: K, value: V): this {
    super.delete(key);
    super.set(key, value);
    while (this.size > this.capacity) {
      const oldest = this.keys().next();
      if (oldest.done) {
        break;
      }
      super.delete(oldest.value);
    }
    return this;
  }
}
