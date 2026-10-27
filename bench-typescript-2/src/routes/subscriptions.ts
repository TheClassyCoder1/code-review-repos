import { Request, Response, Router } from 'express';
import { pool, withTransaction } from '../db';
import { asyncHandler } from '../middleware/asyncHandler';
import { loadSubscription } from '../middleware/loadSubscription';
import { formatCents } from '../money';
import { findPlan } from '../services/planService';
import { applySeatChange, changePlan, pauseSubscription, subscribe } from '../services/subscriptionService';
import { listMeters, meterSummary } from '../services/usageService';
import { Subscription } from '../types';
import { changePlanSchema, createSubscriptionSchema, parsePagination, seatChangeSchema } from '../validators';

export async function listMySubscriptions(req: Request, res: Response): Promise<void> {
  const { limit, offset } = parsePagination(req.query);
  const { rows } = await pool.query<Subscription>(
    `SELECT * FROM subscriptions WHERE account_id = $1
      ORDER BY current_period_end DESC LIMIT $2 OFFSET $3`,
    [req.account!.id, limit, offset],
  );
  res.json({ data: rows, limit, offset });
}

async function createSubscription(req: Request, res: Response): Promise<void> {
  const input = createSubscriptionSchema.parse(req.body);
  const subscription = await subscribe(req.account!.id, input.customerId, input.planId, input.seats);
  res.status(201).json(subscription);
}

export async function subscriptionSummary(req: Request, res: Response): Promise<void> {
  const subscription = res.locals.subscription as Subscription;
  const plan = await findPlan(subscription.account_id, subscription.plan_id);
  const meters = await listMeters(subscription.account_id, subscription.id);
  res.json({
    id: subscription.id,
    status: subscription.status,
    seats: subscription.seats,
    plan: plan ? { code: plan.code, price: formatCents(plan.amount_cents * subscription.seats, plan.currency) } : null,
    periodEnd: subscription.current_period_end,
    meters: meters.map(meterSummary),
  });
}

async function updateSeats(req: Request, res: Response): Promise<void> {
  const { delta } = seatChangeSchema.parse(req.body);
  const subscription = res.locals.subscription as Subscription;
  const seats = await withTransaction(async (client) => {
    await client.query('SELECT id FROM subscriptions WHERE id = $1 FOR UPDATE', [subscription.id]);
    return applySeatChange(client, subscription.id, delta);
  });
  res.json({ id: subscription.id, seats });
}

async function updatePlan(req: Request, res: Response): Promise<void> {
  const { planId } = changePlanSchema.parse(req.body);
  const updated = await changePlan(req.account!.id, res.locals.subscription as Subscription, planId);
  res.json(updated);
}

export const subscriptionRouter = Router();

subscriptionRouter.get('/', asyncHandler(listMySubscriptions));
subscriptionRouter.post('/', asyncHandler(createSubscription));
subscriptionRouter.get('/:subscriptionId', asyncHandler(loadSubscription), asyncHandler(subscriptionSummary));
subscriptionRouter.post('/:subscriptionId/seats', asyncHandler(loadSubscription), asyncHandler(updateSeats));
subscriptionRouter.post('/:subscriptionId/plan', asyncHandler(loadSubscription), asyncHandler(updatePlan));
subscriptionRouter.post(
  '/:subscriptionId/pause',
  asyncHandler(async (req, res) => {
    res.json(await pauseSubscription(req.account!.id, req.params.subscriptionId));
  }),
);
