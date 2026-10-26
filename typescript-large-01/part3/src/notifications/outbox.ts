import { randomUUID } from 'crypto';
import { describeError } from '../util/errors';
import { Logger } from '../util/logger';

export interface Notice {
  loanId: string;
  template: 'DUNNING_D15' | 'DUNNING_D30' | 'DUNNING_D60' | 'FINAL_DEMAND';
  channel: 'EMAIL' | 'SMS' | 'LETTER';
}

export interface OutboxRow extends Notice {
  outboxId: string;
  attempts: number;
  deliveredAt: string | null;
  createdAt: string;
}

export interface OutboxStore {
  insert(row: OutboxRow): Promise<void>;
  markDelivered(outboxId: string, at: string): Promise<void>;
  bumpAttempts(outboxId: string): Promise<void>;
  pending(limit: number): Promise<OutboxRow[]>;
}

export interface NoticeTransport {
  deliver(row: OutboxRow): Promise<void>;
}

/**
 * Transactional outbox for borrower notices. enqueue() persists the notice;
 * dispatch() is only a first delivery attempt. Any row that is not marked
 * delivered stays pending and OutboxRelay retries it until it goes through.
 */
export class NoticeOutbox {
  constructor(
    private readonly store: OutboxStore,
    private readonly transport: NoticeTransport,
  ) {}

  async enqueue(notice: Notice): Promise<OutboxRow> {
    const row: OutboxRow = { ...notice, outboxId: randomUUID(), attempts: 0, deliveredAt: null, createdAt: new Date().toISOString() };
    await this.store.insert(row);
    return row;
  }

  async dispatch(row: OutboxRow): Promise<void> {
    await this.attempt(row);
  }

  async attempt(row: OutboxRow): Promise<void> {
    await this.store.bumpAttempts(row.outboxId);
    await this.transport.deliver(row);
    await this.store.markDelivered(row.outboxId, new Date().toISOString());
  }
}

export class OutboxRelay {
  private timer: NodeJS.Timeout | null = null;

  constructor(
    private readonly outbox: NoticeOutbox,
    private readonly store: OutboxStore,
    private readonly log: Logger,
    private readonly batchSize = 100,
  ) {}

  async drainOnce(): Promise<number> {
    const rows = await this.store.pending(this.batchSize);
    let delivered = 0;
    for (const row of rows) {
      try {
        await this.outbox.attempt(row);
        delivered++;
      } catch (err) {
        this.log.warn('outbox redelivery failed', { outboxId: row.outboxId, attempts: row.attempts + 1, error: describeError(err) });
      }
    }
    return delivered;
  }

  start(intervalMs: number): void {
    if (this.timer) {
      return;
    }
    this.timer = setInterval(() => {
      this.drainOnce().catch((err) => this.log.error('outbox relay tick failed', { error: describeError(err) }));
    }, intervalMs);
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }
}
