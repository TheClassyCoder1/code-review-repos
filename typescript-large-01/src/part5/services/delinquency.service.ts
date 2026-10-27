import {
  Cents,
  DelinquencyBucket,
  IsoDate,
  RepaymentSchedule,
  ServicedLoan,
} from '../domain/types';
import { daysBetween } from '../util/business-calendar';
import { outstandingFor } from './payment-allocation.service';

export interface DelinquencySnapshot {
  loanId: number;
  daysPastDue: number;
  amountPastDue: Cents;
  bucket: DelinquencyBucket;
}

export interface PortfolioSummary {
  asOf: IsoDate;
  loanCount: number;
  byBucket: Record<DelinquencyBucket, { count: number; amountPastDue: Cents }>;
  delinquencyRate: number; // share of loans 30+ days past due, 0..1
}

/** Payments under this many cents short of the installment don't trip delinquency. */
const DE_MINIMIS_SHORTFALL: Cents = 500;

export class DelinquencyService {
  /**
   * Days past due is measured from the oldest installment that is still unpaid beyond
   * the de-minimis tolerance and whose due date is strictly before `asOf`.
   */
  computeDaysPastDue(schedule: RepaymentSchedule, asOf: IsoDate): { days: number; amount: Cents } {
    let oldestUnpaid: IsoDate | undefined;
    let amount = 0;

    for (const inst of schedule.installments) {
      if (daysBetween(inst.dueDate, asOf) <= 0) {
        continue;
      }
      const o = outstandingFor(inst);
      const owed = o.fees + o.interest + o.principal;
      if (owed <= 0) {
        continue;
      }
      amount += owed;
      if (owed > DE_MINIMIS_SHORTFALL && (!oldestUnpaid || inst.dueDate < oldestUnpaid)) {
        oldestUnpaid = inst.dueDate;
      }
    }

    if (!oldestUnpaid) {
      return { days: 0, amount };
    }
    return { days: daysBetween(oldestUnpaid, asOf), amount };
  }

  bucketFor(daysPastDue: number): DelinquencyBucket {
    if (!Number.isInteger(daysPastDue) || daysPastDue < 0) {
      throw new RangeError(`daysPastDue must be a non-negative integer, got ${daysPastDue}`);
    }
    if (daysPastDue === 0) {
      return DelinquencyBucket.Current;
    }
    if (daysPastDue < 30) {
      return DelinquencyBucket.Dpd1To29;
    }
    if (daysPastDue < 60) {
      return DelinquencyBucket.Dpd30To59;
    }
    if (daysPastDue < 90) {
      return DelinquencyBucket.Dpd60To89;
    }
    return DelinquencyBucket.Dpd90Plus;
  }

  snapshot(loan: ServicedLoan, schedule: RepaymentSchedule, asOf: IsoDate): DelinquencySnapshot {
    const { days, amount } = this.computeDaysPastDue(schedule, asOf);
    return { loanId: loan.loanId, daysPastDue: days, amountPastDue: amount, bucket: this.bucketFor(days) };
  }

  summarizePortfolio(snapshots: readonly DelinquencySnapshot[], asOf: IsoDate): PortfolioSummary {
    const byBucket = {} as PortfolioSummary['byBucket'];
    for (const bucket of Object.values(DelinquencyBucket)) {
      byBucket[bucket] = { count: 0, amountPastDue: 0 };
    }

    let seriouslyDelinquent = 0;
    for (const snap of snapshots) {
      const entry = byBucket[snap.bucket];
      entry.count += 1;
      entry.amountPastDue += snap.amountPastDue;
      if (snap.daysPastDue >= 30) {
        seriouslyDelinquent += 1;
      }
    }

    return {
      asOf,
      loanCount: snapshots.length,
      byBucket,
      delinquencyRate: snapshots.length === 0 ? 0 : seriouslyDelinquent / snapshots.length,
    };
  }
}
