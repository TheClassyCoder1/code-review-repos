export type MessageHandler<M> = (message: M) => Promise<void>;

export interface DispatchStats {
  delivered: number;
  failed: number;
  pending: number;
}

/**
 * Feeds messages to a handler strictly one at a time, in the order they were
 * enqueued. The next message is not handed over until the previous handler
 * promise has settled, so a handler never observes two of its own calls
 * in flight at once.
 *
 * Backpressure is the caller's job: `enqueue` rejects once `maxPending` is
 * reached so the consumer can pause the partition instead of buffering
 * without bound.
 */
export class SerialDispatcher<M> {
  private readonly queue: M[] = [];
  private draining = false;
  private idleWaiters: Array<() => void> = [];
  private readonly stats: DispatchStats = { delivered: 0, failed: 0, pending: 0 };

  constructor(
    private readonly handler: MessageHandler<M>,
    private readonly maxPending: number,
    private readonly onError: (error: unknown, message: M) => void,
  ) {}

  enqueue(message: M): boolean {
    if (this.queue.length >= this.maxPending) {
      return false;
    }
    this.queue.push(message);
    this.stats.pending = this.queue.length;
    if (!this.draining) {
      this.draining = true;
      void this.drain();
    }
    return true;
  }

  /** Resolves once every message enqueued so far has been handled. */
  whenIdle(): Promise<void> {
    if (!this.draining && this.queue.length === 0) {
      return Promise.resolve();
    }
    return new Promise((resolve) => this.idleWaiters.push(resolve));
  }

  snapshot(): DispatchStats {
    return { ...this.stats };
  }

  private async drain(): Promise<void> {
    try {
      while (this.queue.length > 0) {
        const message = this.queue.shift() as M;
        this.stats.pending = this.queue.length;
        try {
          await this.handler(message);
          this.stats.delivered += 1;
        } catch (error) {
          this.stats.failed += 1;
          try {
            this.onError(error, message);
          } catch {
            // an error reporter must never stall the queue
          }
        }
      }
    } finally {
      this.draining = false;
      const waiters = this.idleWaiters;
      this.idleWaiters = [];
      waiters.forEach((resolve) => resolve());
    }
  }
}
