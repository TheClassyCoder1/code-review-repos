// Date helpers for portal-facing payloads. All output is in UTC so that a statement
// rendered in two regions shows the same calendar day.

function pad2(value: number): string {
  return value < 10 ? `0${value}` : String(value);
}

/** Formats a date as `YYYY-MM-DD` (UTC). */
export function formatIsoDay(date: Date): string {
  return `${date.getUTCFullYear()}-${pad2(date.getUTCMonth() + 1)}-${pad2(date.getUTCDate())}`;
}

/** Formats a statement due date for the onboarding and statement views (UTC calendar day). */
export function formatDueDate(value: Date): string {
  const year = value.getUTCFullYear();
  const month = String(value.getUTCMonth() + 1).padStart(2, '0');
  const day = String(value.getUTCDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Whole days between two instants, ignoring the time of day. */
export function daysBetween(from: Date, to: Date): number {
  const start = Date.UTC(from.getUTCFullYear(), from.getUTCMonth(), from.getUTCDate());
  const end = Date.UTC(to.getUTCFullYear(), to.getUTCMonth(), to.getUTCDate());
  return Math.round((end - start) / 86_400_000);
}

/** Adds whole days to a date without mutating the input. */
export function addDays(date: Date, days: number): Date {
  const copy = new Date(date.getTime());
  copy.setUTCDate(copy.getUTCDate() + days);
  return copy;
}
