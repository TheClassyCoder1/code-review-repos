import { Request, Response, Router } from 'express';
import { asyncHandler } from '../middleware/asyncHandler';
import { exportUsageEvents, findMeter, incrementMeter, recordUsageEvent } from '../services/usageService';
import { usageEventSchema } from '../validators';

async function postUsageEvent(req: Request, res: Response): Promise<void> {
  const event = usageEventSchema.parse(req.body);
  const meter = await findMeter(req.account!.id, event.meterId);
  if (!meter) {
    res.status(404).json({ error: 'meter not found' });
    return;
  }
  const recorded = await recordUsageEvent(event);
  const total = recorded ? await incrementMeter(meter.id, event.quantity) : meter.quantity;
  res.status(recorded ? 201 : 200).json({ meterId: meter.id, quantity: total, duplicate: !recorded });
}

const CSV_HEADER = 'meter_id,quantity,occurred_at,idempotency_key';

export async function importUsageCsv(req: Request, res: Response): Promise<void> {
  const text = (req.body as Buffer).toString('utf8');
  const [header, ...lines] = text.split(/\r?\n/).filter((line) => line.trim() !== '');
  if (header !== CSV_HEADER) {
    res.status(400).json({ error: `expected header ${CSV_HEADER}` });
    return;
  }
  let accepted = 0;
  for (const line of lines) {
    const [meterId, quantity, occurredAt, idempotencyKey] = line.split(',');
    const event = usageEventSchema.parse({ meterId, quantity: Number(quantity), occurredAt, idempotencyKey });
    const meter = await findMeter(req.account!.id, event.meterId);
    if (meter && (await recordUsageEvent(event))) {
      await incrementMeter(meter.id, event.quantity);
      accepted += 1;
    }
  }
  res.json({ received: lines.length, accepted });
}

async function downloadUsage(req: Request, res: Response): Promise<void> {
  const meter = await findMeter(req.account!.id, req.params.meterId);
  if (!meter) {
    res.status(404).json({ error: 'meter not found' });
    return;
  }
  res.type('text/csv').send(await exportUsageEvents(meter.id));
}

export const usageRouter = Router();

usageRouter.post('/events', asyncHandler(postUsageEvent));
usageRouter.post('/import', asyncHandler(importUsageCsv));
usageRouter.get('/meters/:meterId/export', asyncHandler(downloadUsage));
