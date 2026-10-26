import { Logger } from '../util/logger';

export interface RecoveryEvent {
  eventId: string;
  agentId: string;
  loanId: string;
  amountMinor: number;
}

export interface AuditTrail {
  append(entry: { type: string; agentId: string; loanId: string; amountMinor: number; eventId: string }): Promise<void>;
}

/**
 * Running recovered-amount totals per collections agent, used for the live
 * leaderboard and the monthly commission run. Recovery events are consumed in
 * batches and each batch is processed in parallel.
 */
export class CollectionsTally {
  private readonly totals = new Map<string, number>();
  private readonly seen = new Set<string>();

  constructor(
    private readonly audit: AuditTrail,
    private readonly log: Logger,
  ) {}

  async recordRecovery(event: RecoveryEvent): Promise<void> {
    if (this.seen.has(event.eventId)) {
      return;
    }
    this.seen.add(event.eventId);
    const current = this.totals.get(event.agentId) ?? 0;
    await this.audit.append({
      type: 'RECOVERY',
      agentId: event.agentId,
      loanId: event.loanId,
      amountMinor: event.amountMinor,
      eventId: event.eventId,
    });
    this.totals.set(event.agentId, current + event.amountMinor);
  }

  async consumeBatch(events: RecoveryEvent[]): Promise<void> {
    await Promise.all(events.map((e) => this.recordRecovery(e)));
    this.log.info('recovery batch applied', { size: events.length });
  }

  totalFor(agentId: string): number {
    return this.totals.get(agentId) ?? 0;
  }
}
