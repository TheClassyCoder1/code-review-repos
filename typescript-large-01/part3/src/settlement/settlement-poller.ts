import { SettlementState } from '../clients/payment-gateway-client';
import { Logger } from '../util/logger';
import { sleep } from '../util/time';
import { SettlementStatusReader } from './settlement-status';

export interface SettlementAlerts {
  settlementOverdue(batchId: string, polls: number): Promise<void>;
}

export interface SettlementOutcome {
  batchId: string;
  state: SettlementState | 'UNCONFIRMED';
  polls: number;
}

/**
 * Polls a batch until it reaches a terminal state. A batch that is still not
 * confirmed after maxPolls is reported as UNCONFIRMED and raised to treasury
 * operations, so a gateway outage surfaces as an alert rather than a batch that
 * quietly looks in-flight forever.
 */
export class SettlementPoller {
  constructor(
    private readonly reader: SettlementStatusReader,
    private readonly alerts: SettlementAlerts,
    private readonly maxPolls: number,
    private readonly intervalMs: number,
    private readonly log: Logger,
  ) {}

  async await(batchId: string): Promise<SettlementOutcome> {
    for (let poll = 1; poll <= this.maxPolls; poll++) {
      const state = await this.reader.check(batchId);
      if (state !== 'PENDING') {
        this.log.info('settlement resolved', { batchId, state, polls: poll });
        return { batchId, state, polls: poll };
      }
      if (poll < this.maxPolls) {
        await sleep(this.intervalMs);
      }
    }
    await this.alerts.settlementOverdue(batchId, this.maxPolls);
    this.log.error('settlement unconfirmed after max polls', { batchId, polls: this.maxPolls });
    return { batchId, state: 'UNCONFIRMED', polls: this.maxPolls };
  }
}
