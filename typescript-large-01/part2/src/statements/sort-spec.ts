import { HttpError } from '../http/respond';

/** Maps the public sort keys onto the reporting schema's column names. */
const SORT_COLUMNS = {
  period: 'period',
  due: 'total_due_cents',
  loan: 'loan_id',
  issued: 'issued_at',
} as const;

export type SortColumn = (typeof SORT_COLUMNS)[keyof typeof SORT_COLUMNS];
export type SortDirection = 'ASC' | 'DESC';

export interface SortSpec {
  column: SortColumn;
  direction: SortDirection;
}

/**
 * Parses `?sort=due:desc`. Only keys in SORT_COLUMNS are accepted and the
 * direction is normalised to ASC/DESC, so the result is safe to place in an
 * ORDER BY clause.
 */
export function parseSortSpec(raw: string | null): SortSpec {
  if (!raw) return { column: 'period', direction: 'DESC' };
  const [key, dir = 'asc'] = raw.split(':', 2);
  if (!Object.prototype.hasOwnProperty.call(SORT_COLUMNS, key)) {
    throw new HttpError(400, 'unsupported sort key');
  }
  const lowered = dir.toLowerCase();
  if (lowered !== 'asc' && lowered !== 'desc') {
    throw new HttpError(400, 'unsupported sort direction');
  }
  return {
    column: SORT_COLUMNS[key as keyof typeof SORT_COLUMNS],
    direction: lowered === 'asc' ? 'ASC' : 'DESC',
  };
}
