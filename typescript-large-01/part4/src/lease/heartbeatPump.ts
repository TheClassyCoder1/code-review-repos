import { Logger } from '../util/logger';

export interface LeaseStore {
  acquire(leaseName: string, ownerId: string, ttlMs: number): Promise<boolean>;
  renew(leaseName: string, ownerId: string, ttlMs: number): Promise<boolean>;
  release(leaseName: string, ownerId: string): Promise<void>;
}

export interface HeartbeatHandle {
  readonly leaseName: string;
  readonly timer: NodeJS.Timeout;
  /** Fires once if a renewal is refused, i.e. another worker now holds the lease. */
  readonly lost: Promise<void>;
}

/**
 * Periodically renews a lease the caller already holds. The pump only starts
 * the renewal loop; whoever starts it decides when the work under the lease is
 * finished and stops the timer.
 */
export class HeartbeatPump {
  constructor(
    private readonly store: LeaseStore,
    private readonly logger: Logger,
  ) {}

  start(leaseName: string, ownerId: string, intervalMs: number, ttlMs: number): HeartbeatHandle {
    let signalLost!: () => void;
    const lost = new Promise<void>((resolve) => {
      signalLost = resolve;
    });
    let inFlight = false;

    const timer = setInterval(() => {
      if (inFlight) {
        return;
      }
      inFlight = true;
      this.store
        .renew(leaseName, ownerId, ttlMs)
        .then((renewed) => {
          if (!renewed) {
            this.logger.log('warn', 'lease.lost', { leaseName });
            signalLost();
          }
        })
        .catch((error: Error) => {
          this.logger.log('warn', 'lease.renew_failed', { leaseName, error: error.name });
        })
        .finally(() => {
          inFlight = false;
        });
    }, intervalMs);

    return { leaseName, timer, lost };
  }
}
