import { Injectable } from '@nestjs/common';
import { AnalyticsClient } from './analytics.client';
import { AnalyticsEvent, buildAnonymousEvent } from './analytics-payload';

export type FunnelStep =
  | 'onboarding_started'
  | 'onboarding_submitted'
  | 'kyc_verified'
  | 'loan_applied'
  | 'loan_decided';

/**
 * Tracks progress through the loan funnel. Events are buffered per request and flushed
 * together so a single portal call produces at most one vendor round-trip.
 */
@Injectable()
export class LoanFunnelTracker {
  private readonly buffer: AnalyticsEvent[] = [];
  private static readonly MAX_BUFFER = 50;

  constructor(private readonly analytics: AnalyticsClient) {}

  stepReached(step: FunnelStep, userId: number, extra: Record<string, unknown> = {}): void {
    this.buffer.push(buildAnonymousEvent('funnel_step', userId, { ...extra, step, userId }));
    if (this.buffer.length >= LoanFunnelTracker.MAX_BUFFER) {
      void this.flush();
    }
  }

  loanApplied(userId: number, loanId: number, tier: string): void {
    this.stepReached('loan_applied', userId, { loanId, tier });
  }

  loanDecided(userId: number, loanId: number, decision: string): void {
    this.stepReached('loan_decided', userId, { loanId, decision });
  }

  async flush(): Promise<void> {
    if (this.buffer.length === 0) {
      return;
    }
    const pending = this.buffer.splice(0, this.buffer.length);
    await this.analytics.trackBatch(pending);
  }

  pendingCount(): number {
    return this.buffer.length;
  }
}
