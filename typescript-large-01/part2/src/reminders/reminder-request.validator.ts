import { HttpError } from '../http/respond';

export interface ReminderRequest {
  loanId: number;
  greetingName: string;
  dueDate: string;
  amountDueCents: number;
}

/** Letters (any script), spaces, apostrophes, periods and hyphens only. */
const GREETING_NAME = /^[\p{L}\p{M} .'-]{1,60}$/u;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

const isCount = (v: unknown): v is number => typeof v === 'number' && Number.isInteger(v) && v >= 0;

/**
 * Validates a payment-reminder request. The greeting name ends up inside the
 * reminder HTML, so anything beyond plain name characters is refused here.
 */
export function validateReminderRequest(body: unknown): ReminderRequest {
  const b = (body ?? {}) as Record<string, unknown>;
  if (!isCount(b.loanId) || b.loanId === 0) {
    throw new HttpError(400, 'loanId must be a positive integer');
  }
  const greetingName = typeof b.greetingName === 'string' ? b.greetingName.trim() : '';
  if (!GREETING_NAME.test(greetingName)) {
    throw new HttpError(400, 'greetingName contains unsupported characters');
  }
  const dueDate = b.dueDate;
  if (typeof dueDate !== 'string' || !ISO_DATE.test(dueDate) || Number.isNaN(Date.parse(dueDate))) {
    throw new HttpError(400, 'dueDate must be an ISO date (YYYY-MM-DD)');
  }
  if (!isCount(b.amountDueCents)) {
    throw new HttpError(400, 'amountDueCents must be a non-negative integer');
  }
  return { loanId: b.loanId, greetingName, dueDate, amountDueCents: b.amountDueCents };
}
