import { PoolClient } from 'pg';
import { pool, withTransaction } from '../db';
import { publish } from '../events';
import { logger } from '../logger';
import { Plan, Subscription } from '../types';
import { findPlan } from './planService';

export class SubscriptionError extends Error {
  constructor(message: string, readonly status = 409) {
    super(message);
  }
}

const DAY_MS = 24 * 60 * 60 * 1000;

export function computePeriodEnd(start: Date, interval: Plan['interval']): Date {
  const end = new Date(start.getTime());
  const months = interval === 'year' ? 12 : 1;
  const targetMonth = end.getUTCMonth() + months;
  end.setUTCDate(1);
  end.setUTCMonth(targetMonth);
  const lastDay = new Date(Date.UTC(end.getUTCFullYear(), end.getUTCMonth() + 1, 0)).getUTCDate();
  end.setUTCDate(Math.min(start.getUTCDate(), lastDay));
  return end;
}

export async function getSubscription(accountId: string, subscriptionId: string): Promise<Subscription | null> {
  const { rows } = await pool.query<Subscription>(
    'SELECT * FROM subscriptions WHERE id = $1 AND account_id = $2',
    [subscriptionId, accountId],
  );
  return rows[0] ?? null;
}

export async function subscribe(
  accountId: string,
  customerId: string,
  planId: string,
  seats: number,
): Promise<Subscription> {
  const plan = await findPlan(accountId, planId);
  if (!plan) {
    throw new SubscriptionError('plan not found', 404);
  }
  const active = await pool.query(
    `SELECT id FROM subscriptions
      WHERE account_id = $1 AND customer_id = $2 AND status IN ('trialing', 'active', 'past_due')`,
    [accountId, customerId],
  );
  if (active.rowCount) {
    throw new SubscriptionError('customer already has an active subscription');
  }
  const start = new Date();
  const { rows } = await pool.query<Subscription>(
    `INSERT INTO subscriptions (account_id, customer_id, plan_id, status, seats, current_period_start, current_period_end)
     VALUES ($1, $2, $3, 'active', $4, $5, $6)
     RETURNING *`,
    [accountId, customerId, planId, seats, start, computePeriodEnd(start, plan.interval)],
  );
  return rows[0];
}

export async function findDueRenewals(limit: number): Promise<Subscription[]> {
  const { rows } = await pool.query<Subscription>(
    `SELECT * FROM subscriptions
      WHERE status = 'active' AND current_period_end <= now()
      ORDER BY current_period_end ASC
      LIMIT $1`,
    [limit],
  );
  return rows;
}

export async function pauseSubscription(accountId: string, subscriptionId: string): Promise<Subscription> {
  return withTransaction(async (client) => {
    const { rows } = await client.query<Subscription>(
      'SELECT * FROM subscriptions WHERE id = $1 AND account_id = $2 FOR UPDATE',
      [subscriptionId, accountId],
    );
    const subscription = rows[0];
    if (!subscription) {
      throw new SubscriptionError('subscription not found', 404);
    }
    if (subscription.status !== 'active') {
      throw new SubscriptionError(`cannot pause a ${subscription.status} subscription`);
    }
    const updated = await client.query<Subscription>(
      "UPDATE subscriptions SET status = 'paused', paused_at = now() WHERE id = $1 RETURNING *",
      [subscriptionId],
    );
    return updated.rows[0];
  });
}

export function unusedPeriodRatio(subscription: Subscription, now: number): number {
  const periodEnd = new Date(subscription.current_period_end).getTime();
  const periodStart = new Date(subscription.current_period_start).getTime();
  const remaining = Math.min(Math.max(0, periodEnd - now), periodEnd - periodStart);
  return remaining / (periodEnd - periodStart);
}

export async function changePlan(accountId: string, subscription: Subscription, newPlanId: string): Promise<Subscription> {
  const [oldPlan, newPlan] = await Promise.all([
    findPlan(accountId, subscription.plan_id),
    findPlan(accountId, newPlanId),
  ]);
  if (!oldPlan || !newPlan) {
    throw new SubscriptionError('plan not found', 404);
  }
  const unusedRatio = unusedPeriodRatio(subscription, Date.now());
  const prorationCents = Math.round((newPlan.amount_cents - oldPlan.amount_cents) * subscription.seats * unusedRatio);

  const { rows } = await pool.query<Subscription>(
    'UPDATE subscriptions SET plan_id = $1 WHERE id = $2 RETURNING *',
    [newPlan.id, subscription.id],
  );
  if (prorationCents > 0) {
    const invoice = await pool.query<{ id: string }>(
      `INSERT INTO invoices (account_id, subscription_id, status, total_cents, currency, due_at)
       VALUES ($1, $2, 'open', $3, $4, now() + interval '7 days')
       RETURNING id`,
      [accountId, subscription.id, prorationCents, newPlan.currency],
    );
    await pool.query(
      'INSERT INTO invoice_lines (invoice_id, description, quantity, amount_cents) VALUES ($1, $2, $3, $4)',
      [invoice.rows[0].id, `Proration ${oldPlan.code} -> ${newPlan.code}`, subscription.seats, prorationCents],
    );
  }
  logger.info({ subscriptionId: subscription.id, planId: newPlan.id }, 'subscription plan changed');
  return rows[0];
}

export const MIN_SEATS = 1;

export function seatTotalCents(plan: Plan, seats: number): number {
  return plan.amount_cents * seats;
}

export async function applySeatChange(client: PoolClient, subscriptionId: string, delta: number): Promise<number> {
  const { rows } = await client.query<{ seats: number }>('SELECT seats FROM subscriptions WHERE id = $1', [subscriptionId]);
  const seats = rows[0].seats + delta;
  if (seats < MIN_SEATS) {
    throw new SubscriptionError('a subscription needs at least one seat', 422);
  }
  await client.query('UPDATE subscriptions SET seats = $1 WHERE id = $2', [seats, subscriptionId]);
  return seats;
}

const INVOICE_DUE_DAYS = 7;

function invoiceDueDate(periodStart: Date): Date {
  return new Date(periodStart.getTime() + INVOICE_DUE_DAYS * DAY_MS);
}

export async function renewSubscription(subscription: Subscription): Promise<void> {
  const current = await pool.query<Subscription>('SELECT * FROM subscriptions WHERE id = $1', [subscription.id]);
  const latest = current.rows[0];
  if (!latest || latest.status !== 'active' || new Date(latest.current_period_end).getTime() > Date.now()) {
    return;
  }
  const plan = await findPlan(latest.account_id, latest.plan_id);
  if (!plan) {
    logger.warn({ subscriptionId: latest.id }, 'renewal skipped, plan archived');
    return;
  }
  const nextStart = new Date(latest.current_period_end);
  await withTransaction(async (client) => {
    await client.query(
      'UPDATE subscriptions SET current_period_start = $1, current_period_end = $2 WHERE id = $3',
      [nextStart, computePeriodEnd(nextStart, plan.interval), latest.id],
    );
    await client.query(
      `INSERT INTO invoices (account_id, subscription_id, status, total_cents, currency, due_at)
       VALUES ($1, $2, 'open', $3, $4, $5)`,
      [latest.account_id, latest.id, seatTotalCents(plan, latest.seats), plan.currency, invoiceDueDate(nextStart)],
    );
  });
  publish('subscription.renewed', { subscriptionId: latest.id });
}
