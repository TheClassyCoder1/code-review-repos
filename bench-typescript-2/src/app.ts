import express, { NextFunction, Request, Response } from 'express';
import { ZodError } from 'zod';
import { config } from './config';
import { logger } from './logger';
import { asyncHandler } from './middleware/asyncHandler';
import { requireApiKey } from './middleware/auth';
import { customerRouter, invoiceRouter } from './routes/invoices';
import { planRouter } from './routes/plans';
import { reportRouter } from './routes/reports';
import { subscriptionRouter } from './routes/subscriptions';
import { usageRouter } from './routes/usage';
import { webhookRouter } from './routes/webhooks';
import { InvoiceError } from './services/invoiceService';
import { SubscriptionError } from './services/subscriptionService';
import './types';

export function buildApp(): express.Express {
  const app = express();

  app.get('/healthz', (_req, res) => res.json({ ok: true }));
  app.use('/webhooks', express.raw({ type: 'application/json', limit: '256kb' }), webhookRouter);

  const api = express.Router();
  api.use(asyncHandler(requireApiKey));
  api.use('/usage/import', express.raw({ type: 'text/csv', limit: config.limits.usageImportBytes }));
  api.use(express.json({ limit: config.limits.jsonBody }));
  api.use('/plans', planRouter);
  api.use('/customers', customerRouter);
  api.use('/subscriptions', subscriptionRouter);
  api.use('/usage', usageRouter);
  api.use('/invoices', invoiceRouter);
  api.use('/reports', reportRouter);
  app.use('/api/v1', api);

  app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
    if (err instanceof ZodError) {
      res.status(422).json({ error: 'validation failed', issues: err.issues });
      return;
    }
    if (err instanceof SubscriptionError || err instanceof InvoiceError) {
      res.status(err.status).json({ error: err.message });
      return;
    }
    logger.error({ err }, 'unhandled error');
    res.status(500).json({ error: 'internal error' });
  });

  return app;
}
