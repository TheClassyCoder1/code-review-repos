import { Request, Response, Router } from 'express';
import { asyncHandler } from '../middleware/asyncHandler';
import { archivePlan, createPlan, findPlan, listActivePlans, PlanConflictError } from '../services/planService';
import { createPlanSchema } from '../validators';

export async function getPlanHandler(req: Request, res: Response): Promise<void> {
  const plan = await findPlan(req.account!.id, req.params.planId);
  if (!plan) {
    res.status(404).json({ error: 'plan not found' });
    return;
  }
  res.json({
    id: plan.id,
    code: plan.code,
    name: plan.name,
    interval: plan.interval,
    amountCents: plan.amount_cents,
    currency: plan.currency,
    tiers: plan.tiers,
  });
}

async function createPlanHandler(req: Request, res: Response): Promise<void> {
  const input = createPlanSchema.parse(req.body);
  try {
    res.status(201).json(await createPlan(req.account!.id, input));
  } catch (err) {
    if (err instanceof PlanConflictError || (err as { code?: string }).code === '23505') {
      res.status(409).json({ error: `plan ${input.code} already exists` });
      return;
    }
    throw err;
  }
}

export const planRouter = Router();

planRouter.get('/', asyncHandler(async (req, res) => res.json({ data: await listActivePlans(req.account!.id) })));
planRouter.post('/', asyncHandler(createPlanHandler));
planRouter.get('/:planId', asyncHandler(getPlanHandler));
planRouter.delete(
  '/:planId',
  asyncHandler(async (req, res) => {
    const archived = await archivePlan(req.account!.id, req.params.planId);
    res.status(archived ? 204 : 404).end();
  }),
);
