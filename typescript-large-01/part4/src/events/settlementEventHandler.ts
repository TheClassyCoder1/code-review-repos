import { DisbursementRepository } from '../disbursement/ledgerRepository';
import { applyTransition, isTerminal } from '../domain/statusMachine';
import { Logger } from '../util/logger';

export interface SettlementEvent {
  eventId: string;
  disbursementId: string;
  gatewayReference: string;
  outcome: 'SETTLED' | 'FAILED';
  occurredAt: string;
}

const SEEN_CAPACITY = 50_000;

/**
 * Applies gateway settlement callbacks to the disbursement ledger.
 *
 * The gateway delivers at-least-once, so the same event id can arrive more
 * than once; recently handled ids are remembered and replays are skipped.
 */
export class SettlementEventHandler {
  private readonly seen = new Set<string>();

  constructor(
    private readonly repository: DisbursementRepository,
    private readonly logger: Logger,
  ) {}

  async handle(event: SettlementEvent): Promise<void> {
    if (this.seen.has(event.eventId)) {
      this.logger.log('debug', 'settlement.replay_skipped', { eventId: event.eventId });
      return;
    }

    const record = await this.repository.findById(event.disbursementId);
    if (!record) {
      this.logger.log('warn', 'settlement.unknown_disbursement', { eventId: event.eventId });
      this.remember(event.eventId);
      return;
    }
    if (isTerminal(record.status)) {
      this.remember(event.eventId);
      return;
    }

    const next = applyTransition(record, event.outcome, event.occurredAt, event.gatewayReference);
    await this.repository.update(next);
    this.remember(event.eventId);
    this.logger.log('info', 'settlement.applied', {
      eventId: event.eventId,
      disbursementId: record.disbursementId,
      status: next.status,
    });
  }

  private remember(eventId: string): void {
    if (this.seen.size >= SEEN_CAPACITY) {
      const oldest = this.seen.values().next().value;
      if (oldest !== undefined) {
        this.seen.delete(oldest);
      }
    }
    this.seen.add(eventId);
  }
}
