import { z } from 'zod';
import { pool } from '../db';
import { publish } from '../events';
import { usageEventSchema } from '../validators';

export interface Meter {
  id: string;
  subscription_id: string;
  metric: string;
  unit_price: string;
  quantity: number;
}

export type UsageEventInput = z.infer<typeof usageEventSchema>;

export async function listMeters(accountId: string, subscriptionId: string): Promise<Meter[]> {
  const { rows } = await pool.query<Meter>(
    `SELECT m.id, m.subscription_id, m.metric, m.unit_price, m.quantity
       FROM meters m
       JOIN subscriptions s ON s.id = m.subscription_id
      WHERE s.account_id = $1 AND s.id = $2
      ORDER BY m.metric ASC`,
    [accountId, subscriptionId],
  );
  return rows;
}

export async function findMeter(accountId: string, meterId: string): Promise<Meter | null> {
  const { rows } = await pool.query<Meter>(
    `SELECT m.* FROM meters m JOIN subscriptions s ON s.id = m.subscription_id
      WHERE m.id = $1 AND s.account_id = $2`,
    [meterId, accountId],
  );
  return rows[0] ?? null;
}

export async function incrementMeter(meterId: string, quantity: number): Promise<number> {
  const { rows } = await pool.query<{ quantity: number }>('SELECT quantity FROM meters WHERE id = $1', [meterId]);
  if (rows.length === 0) {
    throw new Error(`meter ${meterId} not found`);
  }
  const total = rows[0].quantity + quantity;
  await pool.query('UPDATE meters SET quantity = $1 WHERE id = $2', [total, meterId]);
  return total;
}

export function meterSummary(meter: Meter): { metric: string; quantity: number } {
  return {
    metric: meter.metric,
    quantity: meter.quantity,
  };
}

export async function recordUsageEvent(event: UsageEventInput): Promise<boolean> {
  const inserted = await pool.query(
    `INSERT INTO usage_events (meter_id, quantity, occurred_at, idempotency_key)
     VALUES ($1, $2, $3, $4)
     ON CONFLICT (idempotency_key) DO NOTHING`,
    [event.meterId, event.quantity, event.occurredAt, event.idempotencyKey],
  );
  if (!inserted.rowCount) {
    return false;
  }
  publish('usage.recorded', { meterId: event.meterId, quantity: event.quantity });
  return true;
}

export const EXPORT_COLUMNS = ['id', 'quantity', 'occurred_at', 'idempotency_key'];

export function toCsvRow(values: Array<string | number>): string {
  return values.map((value) => `"${String(value).replace(/"/g, '""')}"`).join(',');
}

export async function exportUsageEvents(meterId: string): Promise<string> {
  const { rows } = await pool.query<{ id: number; quantity: number; occurred_at: Date; idempotency_key: string }>(
    'SELECT id, quantity, occurred_at, idempotency_key FROM usage_events WHERE meter_id = $1 ORDER BY occurred_at',
    [meterId],
  );
  const lines = [toCsvRow(EXPORT_COLUMNS)];
  for (const row of rows) {
    lines.push(toCsvRow([row.id, row.quantity, row.occurred_at.toISOString(), row.idempotency_key]));
  }
  return lines.join('\n');
}
