import { ConsumerSettings } from '../config/settings';
import { Logger } from '../util/logger';
import { SerialDispatcher } from '../util/serialDispatcher';
import { SettlementEvent, SettlementEventHandler } from './settlementEventHandler';

export interface RawRecord {
  partition: number;
  offset: string;
  key: string | null;
  value: Buffer | null;
}

export interface PartitionControl {
  pause(partition: number): void;
  resume(partition: number): void;
  commit(partition: number, offset: string): Promise<void>;
}

const OUTCOMES: ReadonlySet<string> = new Set(['SETTLED', 'FAILED']);

/**
 * Decodes a settlement record from the wire. Envelope version 1 is the only
 * format published today; anything else is rejected so a future producer
 * change is noticed rather than half-parsed.
 */
export function decodeSettlementEnvelope(record: RawRecord): SettlementEvent {
  if (!record.value || record.value.length === 0) {
    throw new Error(`empty settlement record at offset ${record.offset}`);
  }
  const envelope = JSON.parse(record.value.toString('utf8')) as {
    version?: unknown;
    payload?: Record<string, unknown>;
  };
  if (envelope.version !== 1 || typeof envelope.payload !== 'object' || envelope.payload === null) {
    throw new Error(`unsupported settlement envelope at offset ${record.offset}`);
  }

  const { eventId, disbursementId, gatewayReference, outcome, occurredAt } = envelope.payload;
  if (
    typeof eventId !== 'string' ||
    typeof disbursementId !== 'string' ||
    typeof gatewayReference !== 'string' ||
    typeof outcome !== 'string' ||
    !OUTCOMES.has(outcome) ||
    typeof occurredAt !== 'string' ||
    Number.isNaN(Date.parse(occurredAt))
  ) {
    throw new Error(`settlement payload failed validation at offset ${record.offset}`);
  }
  return { eventId, disbursementId, gatewayReference, outcome: outcome as SettlementEvent['outcome'], occurredAt };
}

/**
 * Consumes the settlement topic. All records, regardless of partition, are
 * funnelled through a single SerialDispatcher, so the settlement handler is
 * never invoked concurrently with itself. When the dispatcher's buffer is full
 * the source partition is paused until the backlog drains.
 */
export class SettlementPartitionConsumer {
  private readonly dispatcher: SerialDispatcher<{ record: RawRecord; event: SettlementEvent }>;
  private readonly paused = new Set<number>();

  constructor(
    private readonly handler: SettlementEventHandler,
    private readonly control: PartitionControl,
    private readonly settings: ConsumerSettings,
    private readonly logger: Logger,
  ) {
    this.dispatcher = new SerialDispatcher(
      async ({ record, event }) => {
        await this.handler.handle(event);
        await this.control.commit(record.partition, record.offset);
      },
      this.settings.maxBufferedMessages,
      (error, { record }) =>
        this.logger.log('error', 'settlement.handle_failed', {
          partition: record.partition,
          offset: record.offset,
          error: (error as Error).message,
        }),
    );
  }

  async onRecord(record: RawRecord): Promise<void> {
    let event: SettlementEvent;
    try {
      event = decodeSettlementEnvelope(record);
    } catch (error) {
      this.logger.log('error', 'settlement.decode_failed', { partition: record.partition, offset: record.offset });
      await this.control.commit(record.partition, record.offset);
      return;
    }

    while (!this.dispatcher.enqueue({ record, event })) {
      if (!this.paused.has(record.partition)) {
        this.control.pause(record.partition);
        this.paused.add(record.partition);
      }
      await this.dispatcher.whenIdle();
    }
    if (this.paused.delete(record.partition)) {
      this.control.resume(record.partition);
    }
  }

  async shutdown(): Promise<void> {
    await this.dispatcher.whenIdle();
    this.logger.log('info', 'settlement.consumer_stopped', { topic: this.settings.topic });
  }
}
