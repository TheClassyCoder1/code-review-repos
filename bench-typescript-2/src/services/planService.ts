import { z } from 'zod';
import { pool } from '../db';
import { Plan, PriceTier } from '../types';
import { createPlanSchema } from '../validators';

export class PlanConflictError extends Error {}

export async function listActivePlans(accountId: string): Promise<Plan[]> {
  const { rows } = await pool.query<Plan>(
    `SELECT id, account_id, code, name, interval, amount_cents, currency, tiers
       FROM plans
      WHERE account_id = $1 AND archived_at IS NULL
      ORDER BY amount_cents ASC, code ASC`,
    [accountId],
  );
  return rows;
}

export async function findPlan(accountId: string, planId: string): Promise<Plan | null> {
  const { rows } = await pool.query<Plan>(
    'SELECT * FROM plans WHERE id = $1 AND account_id = $2 AND archived_at IS NULL',
    [planId, accountId],
  );
  return rows[0] ?? null;
}

export async function createPlan(accountId: string, input: z.infer<typeof createPlanSchema>): Promise<Plan> {
  const existing = await pool.query('SELECT 1 FROM plans WHERE account_id = $1 AND code = $2', [accountId, input.code]);
  if (existing.rowCount) {
    throw new PlanConflictError(`plan ${input.code} already exists`);
  }
  const { rows } = await pool.query<Plan>(
    `INSERT INTO plans (account_id, code, name, interval, amount_cents, currency, tiers)
     VALUES ($1, $2, $3, $4, $5, $6, $7)
     RETURNING *`,
    [accountId, input.code, input.name, input.interval, input.amountCents, input.currency, JSON.stringify(input.tiers)],
  );
  return rows[0];
}

export async function archivePlan(accountId: string, planId: string): Promise<boolean> {
  const result = await pool.query(
    'UPDATE plans SET archived_at = now() WHERE id = $1 AND account_id = $2 AND archived_at IS NULL',
    [planId, accountId],
  );
  return (result.rowCount ?? 0) > 0;
}

/**
 * Tiers are ordered ascending; `upTo` is the inclusive upper bound of a tier,
 * and the last tier has `upTo: null`.
 */
export function unitPriceForQuantity(tiers: PriceTier[], quantity: number): number {
  if (tiers.length === 0) {
    throw new Error('plan has no tiers');
  }
  const tier = tiers.find((t) => t.upTo === null || quantity < t.upTo);
  if (!tier) {
    return tiers[tiers.length - 1].unitAmountCents;
  }
  return tier.unitAmountCents;
}
