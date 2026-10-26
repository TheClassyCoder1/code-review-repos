import { randomUUID } from 'crypto';
import { LeaseSettings } from '../config/settings';
import { Logger } from '../util/logger';
import { HeartbeatPump, LeaseStore } from './heartbeatPump';

export class LeaseLostError extends Error {
  constructor(readonly leaseName: string) {
    super(`lease ${leaseName} was taken over while work was running`);
    this.name = 'LeaseLostError';
  }
}

/**
 * Runs singleton batch work (statement exports, nightly reconciliation) under
 * a distributed lease. The heartbeat timer and the lease itself are always
 * released in `finally`, whether the work completes, throws, or loses the lease.
 */
export class LeaseRunner {
  private readonly ownerId = `worker-${randomUUID()}`;

  constructor(
    private readonly store: LeaseStore,
    private readonly pump: HeartbeatPump,
    private readonly settings: LeaseSettings,
    private readonly logger: Logger,
  ) {}

  async runWithLease<T>(leaseName: string, work: (signal: AbortSignal) => Promise<T>): Promise<T | null> {
    const acquired = await this.store.acquire(leaseName, this.ownerId, this.settings.leaseTtlMs);
    if (!acquired) {
      this.logger.log('info', 'lease.busy', { leaseName });
      return null;
    }

    const heartbeat = this.pump.start(
      leaseName,
      this.ownerId,
      this.settings.heartbeatIntervalMs,
      this.settings.leaseTtlMs,
    );
    const abort = new AbortController();
    void heartbeat.lost.then(() => abort.abort(new LeaseLostError(leaseName)));

    try {
      const result = await work(abort.signal);
      if (abort.signal.aborted) {
        throw abort.signal.reason;
      }
      return result;
    } finally {
      clearInterval(heartbeat.timer);
      try {
        await this.store.release(leaseName, this.ownerId);
      } catch (error) {
        this.logger.log('warn', 'lease.release_failed', { leaseName, error: (error as Error).name });
      }
    }
  }
}
