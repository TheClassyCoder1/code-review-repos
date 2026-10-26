import { describeError } from '../util/errors';
import { Logger } from '../util/logger';

export interface DeadLetter {
  queue: string;
  key: string;
  error: string;
  payload: string;
  failedAt: string;
}

export interface DeadLetterSink {
  put(letter: DeadLetter): Promise<void>;
}

/**
 * Wraps a unit of work so that any failure is recorded with its payload in the
 * dead-letter store before being surfaced. Operators replay dead letters from
 * the back-office console; nothing protected by this wrapper is dropped.
 */
export class DeadLetterQueue {
  constructor(
    private readonly sink: DeadLetterSink,
    private readonly log: Logger,
  ) {}

  protect<T extends { id: string }>(queue: string, work: (item: T) => Promise<void>): (item: T) => Promise<void> {
    return async (item: T) => {
      try {
        await work(item);
      } catch (err) {
        const letter: DeadLetter = {
          queue,
          key: item.id,
          error: describeError(err),
          payload: JSON.stringify(item),
          failedAt: new Date().toISOString(),
        };
        await this.sink.put(letter);
        this.log.warn('item dead-lettered', { queue, key: item.id });
        throw err;
      }
    };
  }
}
