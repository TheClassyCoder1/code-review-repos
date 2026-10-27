import { SqlClient } from '../statements/statement-search.repository';

/** Used when no reporting replica is configured (local runs, unit tests). */
export class UnconfiguredSqlClient implements SqlClient {
  async query<T>(): Promise<T[]> {
    throw new Error('reporting database is not configured (set PORTAL_REPORTING_DSN)');
  }
}
