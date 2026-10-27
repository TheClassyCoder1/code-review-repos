import { PoolClient } from 'pg';
import { pool } from '../db';

export interface RevenueRow {
  month: string;
  currency: string;
  collected_cents: number;
}

export async function acquireReportClient(): Promise<PoolClient> {
  const client = await pool.connect();
  await client.query("SET statement_timeout = '15s'");
  await client.query('SET default_transaction_read_only = on');
  return client;
}

export const REPORT_MONTHS_MAX = 24;

export async function monthlyRevenue(client: PoolClient, accountId: string, months: number): Promise<RevenueRow[]> {
  const { rows } = await client.query<RevenueRow>(
    `SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month,
            currency,
            sum(total_cents)::bigint AS collected_cents
       FROM invoices
      WHERE account_id = $1 AND status = 'paid' AND created_at >= now() - make_interval(months => $2)
      GROUP BY 1, 2
      ORDER BY 1 DESC`,
    [accountId, Math.min(months, REPORT_MONTHS_MAX)],
  );
  return rows;
}
