import { z } from 'zod';
import { parseAmountToCents } from './money';

export const createPlanSchema = z.object({
  code: z.string().regex(/^[a-z0-9-]{2,40}$/),
  name: z.string().min(1).max(120),
  interval: z.enum(['month', 'year']),
  amountCents: z.number().int().nonnegative(),
  currency: z.string().length(3).default('usd'),
  tiers: z
    .array(z.object({ upTo: z.number().int().positive().nullable(), unitAmountCents: z.number().int().nonnegative() }))
    .default([]),
});

export const createSubscriptionSchema = z.object({
  customerId: z.string().uuid(),
  planId: z.string().uuid(),
  seats: z.number().int().min(1).max(10000).default(1),
});

export const seatChangeSchema = z.object({
  delta: z.number().int().min(-1000).max(1000),
});

export const changePlanSchema = z.object({
  planId: z.string().uuid(),
});

export const adjustmentSchema = z.object({
  reason: z.string().min(1).max(200),
  amount: z.string().transform((value) => parseAmountToCents(value)),
});

export const usageEventSchema = z.object({
  meterId: z.string().uuid(),
  quantity: z.number().int().positive(),
  occurredAt: z.coerce.date(),
  idempotencyKey: z.string().min(8).max(64),
});

export function parsePagination(query: Record<string, unknown>): { limit: number; offset: number } {
  const rawLimit = Number(query.limit ?? 25);
  const rawOffset = Number(query.offset ?? 0);
  const limit = Number.isInteger(rawLimit) ? Math.min(Math.max(rawLimit, 1), 100) : 25;
  const offset = Number.isInteger(rawOffset) && rawOffset >= 0 ? rawOffset : 0;
  return { limit, offset };
}
