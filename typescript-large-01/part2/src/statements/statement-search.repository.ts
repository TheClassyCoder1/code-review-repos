import { SortSpec } from './sort-spec';

/** Reporting replica access. Values are always bound through `params` ($1, $2, ...). */
export interface SqlClient {
  query<T>(sql: string, params: unknown[]): Promise<T[]>;
}

export interface StatementSummaryRow {
  id: string;
  loan_id: number;
  period: string;
  total_due_cents: number;
  issued_at: string;
}

export interface StatementSearch {
  ownerId: string;
  loanId?: number;
  sort: SortSpec;
  limit: number;
}

/** Statement history for the portal list view, read from the reporting replica. */
export class StatementSearchRepository {
  constructor(private readonly sql: SqlClient) {}

  async search(criteria: StatementSearch): Promise<StatementSummaryRow[]> {
    const params: unknown[] = [criteria.ownerId];
    let where = 'owner_id = $1';
    if (criteria.loanId !== undefined) {
      params.push(criteria.loanId);
      where += ` AND loan_id = $${params.length}`;
    }
    params.push(Math.min(Math.max(criteria.limit, 1), 100));
    const { column, direction } = criteria.sort;
    const sql =
      `SELECT id, loan_id, period, total_due_cents, issued_at FROM statements ` +
      `WHERE ${where} ORDER BY ${column} ${direction} LIMIT $${params.length}`;
    return this.sql.query<StatementSummaryRow>(sql, params);
  }
}
