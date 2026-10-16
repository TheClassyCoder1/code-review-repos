import { pool } from '../db';
import { Meter } from './usageService';

export async function computeUsageCharge(subscriptionId: string): Promise<number> {
  const { rows } = await pool.query<Meter>(
    'SELECT id, metric, unit_price, quantity FROM meters WHERE subscription_id = $1',
    [subscriptionId],
  );
  let totalDollars = 0;
  for (const meter of rows) {
    totalDollars += parseFloat(meter.unit_price) * meter.quantity;
  }
  return Math.round(totalDollars * 100);
}
