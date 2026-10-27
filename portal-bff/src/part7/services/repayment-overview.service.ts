import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { RepaymentClient } from '../clients/repayment.client';
import { RiskTrendClient } from '../clients/risk-trend.client';
import {
  InstallmentRecord,
  RepaymentOverview,
  RepaymentScheduleRecord,
  RiskTrendRecord,
} from '../dto/repayment.dto';

/**
 * AGGREGATING proxy: merges the loan-service schedule with the risk-service score trend.
 * The risk trend is decorative, so a risk-service failure degrades to UNKNOWN instead of failing the page.
 */
@Injectable()
export class RepaymentOverviewService {
  private readonly logger = new Logger(RepaymentOverviewService.name);

  constructor(
    private readonly repaymentClient: RepaymentClient,
    private readonly riskTrendClient: RiskTrendClient,
  ) {}

  async overview(loanId: number, today: Date = new Date()): Promise<RepaymentOverview> {
    const [schedule, trend] = await Promise.all([
      this.repaymentClient.getSchedule(loanId),
      this.safeTrend(loanId),
    ]);
    if (!schedule) {
      throw new NotFoundException(`no repayment schedule for loan ${loanId}`);
    }
    return this.summarize(schedule, trend, today);
  }

  summarize(
    schedule: RepaymentScheduleRecord,
    trend: RiskTrendRecord | null,
    today: Date,
  ): RepaymentOverview {
    const live = schedule.installments.filter((i) => i.status !== 'SUPERSEDED');
    const paid = live.filter((i) => i.status === 'PAID');
    const open = live
      .filter((i) => i.status === 'DUE')
      .sort((a, b) => a.sequenceNo - b.sequenceNo);
    const next = open[0];
    const todayIso = toIsoDate(today);

    return {
      loanId: schedule.loanId,
      nextDue: next ? { dueDate: next.dueDate, amount: round2(next.total) } : null,
      paidCount: paid.length,
      remainingCount: open.length,
      outstanding: round2(schedule.outstanding),
      progressPercent: progress(paid, live),
      overdue: next !== undefined && next.dueDate < todayIso,
      riskDirection: trend?.direction ?? 'UNKNOWN',
    };
  }

  private async safeTrend(loanId: number): Promise<RiskTrendRecord | null> {
    try {
      return await this.riskTrendClient.getTrend(loanId);
    } catch (err) {
      this.logger.warn(`risk trend unavailable for loan ${loanId}: ${(err as Error).message}`);
      return null;
    }
  }
}

function progress(paid: InstallmentRecord[], live: InstallmentRecord[]): number {
  const total = live.reduce((sum, i) => sum + i.principal, 0);
  if (total === 0) {
    return 0;
  }
  const repaid = paid.reduce((sum, i) => sum + i.principal, 0);
  return Math.round((repaid / total) * 1000) / 10;
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}
