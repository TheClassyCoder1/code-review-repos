import cookieParser from 'cookie-parser';
import express, { NextFunction, Request, Response } from 'express';
import { logger } from './logger';
import { requireAuth, requireListingOwner } from './middleware/auth';
import { sanitizeNextParam } from './middleware/redirects';
import { authRouter } from './routes/auth';
import { kycRouter } from './routes/kyc';
import { listingsRouter } from './routes/listings';
import { payoutsRouter } from './routes/payouts';
import { sellersRouter } from './routes/sellers';

export function buildApp(): express.Express {
  const app = express();
  app.disable('x-powered-by');
  app.use(cookieParser());
  app.use(sanitizeNextParam);
  app.use('/api/payouts', payoutsRouter);
  app.use(express.json({ limit: '64kb' }));
  app.use('/api/auth', authRouter);
  app.use('/api/sellers', sellersRouter);
  app.use('/api/kyc', kycRouter);
  app.use('/api/listings/:listingId([0-9a-f-]{36})', requireAuth, requireListingOwner);
  app.use('/api/listings', listingsRouter);
  app.get('/healthz', (_req, res) => {
    res.json({ status: 'ok' });
  });
  app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
    logger.error({ err: err.message }, 'unhandled request error');
    res.status(500).json({ error: 'internal error' });
  });
  return app;
}
