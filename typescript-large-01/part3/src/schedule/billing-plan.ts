import { ValidationError } from '../util/errors';
import { isoDate } from '../util/time';
import { billingDates } from './billing-calendar';

export type BillingFrequency = 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'QUARTERLY';

const FREQUENCIES: ReadonlySet<string> = new Set<BillingFrequency>(['WEEKLY', 'BIWEEKLY', 'MONTHLY', 'QUARTERLY']);

export interface BillingPlanRequest {
  loanId: string;
  firstBillingDate: string;
  maturityDate: string;
  frequency: string;
}

export interface BillingPlan {
  loanId: string;
  frequency: BillingFrequency;
  dates: string[];
}

/**
 * Normalises and checks a frequency coming from the plan row or the API.
 * Only frequencies the calendar knows how to advance are accepted.
 */
export function parseBillingFrequency(raw: string): BillingFrequency {
  const normalized = raw.trim().toUpperCase();
  if (!FREQUENCIES.has(normalized)) {
    throw new ValidationError(`unsupported billing frequency '${raw}'`);
  }
  return normalized as BillingFrequency;
}

function parseDate(raw: string, field: string): Date {
  const date = new Date(`${raw}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) {
    throw new ValidationError(`${field} is not a valid date`);
  }
  return date;
}

export function buildBillingPlan(req: BillingPlanRequest): BillingPlan {
  const frequency = parseBillingFrequency(req.frequency);
  const start = parseDate(req.firstBillingDate, 'firstBillingDate');
  const end = parseDate(req.maturityDate, 'maturityDate');
  if (end <= start) {
    throw new ValidationError(`maturity must be after first billing date for ${req.loanId}`);
  }
  return {
    loanId: req.loanId,
    frequency,
    dates: billingDates(start, end, frequency).map(isoDate),
  };
}
