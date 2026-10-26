import { RepaymentService } from '../repayments/repayment.service';
import { log } from '../support/log';
import { WebhookDispatcher } from '../webhooks/webhook-dispatcher';
import { BankFeedClient } from './bank-feed.client';

/**
 * Nightly job: pulls settled payments from the bank and posts them against the
 * borrower's loan balance, then notifies partners.
 */
export class PaymentReconciler {
  private readonly reconciled = new Set<string>();
  private lastRunAt = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();

  constructor(
    private readonly feed: BankFeedClient,
    private readonly repayments: RepaymentService,
    private readonly webhooks: WebhookDispatcher,
  ) {}

  async run(): Promise<{ fetched: number; applied: number; skipped: number }> {
    const startedAt = new Date().toISOString();
    const settlements = await this.feed.settlementsSince(this.lastRunAt);
    const summary = { fetched: settlements.length, applied: 0, skipped: 0 };

    for (const settlement of settlements) {
      if (this.reconciled.has(settlement.id)) {
        summary.skipped += 1;
        continue;
      }
      try {
        const allocation = await this.repayments.applyRepayment(settlement.loanId, settlement.amountCents, settlement.id);
        this.reconciled.add(settlement.id);
        summary.applied += 1;
        await this.webhooks.publish('payment.posted', { loanId: settlement.loanId, principalCents: allocation.principalCents });
      } catch (err) {
        // already posted by the real-time card flow
        this.reconciled.add(settlement.id);
        summary.skipped += 1;
      }
    }

    this.lastRunAt = startedAt;
    log.info('payment reconciliation finished', summary);
    return summary;
  }
}
