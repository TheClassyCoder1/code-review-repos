import { NextFunction, Request, Response, Router } from 'express';
import express from 'express';
import { requireAuth } from '../middleware/auth';
import {
  createPayrailOnboardingSession,
  deletePayoutAccount,
  getPayoutAccount,
  linkPayoutAccount,
  listPayoutAccounts,
  markAccountFailed,
  markAccountVerified,
  pingWebhookEndpoint,
  saveWebhookUrl,
  verifyPayrailSignature,
} from '../services/payoutService';
import { isAllowedWebhookHost, payoutAccountSchema } from '../validators';

export const payoutsRouter = Router();

async function listAccountsHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    res.json(await listPayoutAccounts(req.user!.sellerId));
  } catch (err) {
    next(err);
  }
}

async function linkAccountHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  const parsed = payoutAccountSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'invalid payout account' });
    return;
  }
  try {
    const id = await linkPayoutAccount(req.user!.sellerId, parsed.data);
    res.status(201).json({ id });
  } catch (err) {
    next(err);
  }
}

async function getAccountHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const account = await getPayoutAccount(req.params.accountId);
    if (!account || account.sellerId !== req.user!.sellerId) {
      res.status(404).json({ error: 'payout account not found' });
      return;
    }
    const { sellerId: _sellerId, ...summary } = account;
    res.json(summary);
  } catch (err) {
    next(err);
  }
}

export async function removePayoutAccountHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const account = await getPayoutAccount(req.params.accountId);
    if (!account) {
      res.status(404).json({ error: 'payout account not found' });
      return;
    }
    await deletePayoutAccount(account.id);
    res.status(204).end();
  } catch (err) {
    next(err);
  }
}

async function registerWebhookHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  const url = typeof req.body?.url === 'string' ? req.body.url : '';
  if (!isAllowedWebhookHost(url)) {
    res.status(400).json({ error: 'webhook host is not allow-listed' });
    return;
  }
  try {
    const account = await getPayoutAccount(req.params.accountId);
    if (!account || account.sellerId !== req.user!.sellerId) {
      res.status(404).json({ error: 'payout account not found' });
      return;
    }
    if (!(await pingWebhookEndpoint(url))) {
      res.status(422).json({ error: 'webhook endpoint did not acknowledge ping' });
      return;
    }
    await saveWebhookUrl(account.id, url);
    res.status(204).end();
  } catch (err) {
    next(err);
  }
}

async function onboardingSessionHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    res.json(await createPayrailOnboardingSession(req.user!.sellerId));
  } catch (err) {
    next(err);
  }
}

async function payrailWebhookHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  if (!verifyPayrailSignature(req.body as Buffer, req.header('Payrail-Signature'))) {
    res.status(401).end();
    return;
  }
  try {
    const event = JSON.parse((req.body as Buffer).toString('utf8')) as { type: string; data: { token: string } };
    if (event.type === 'bank_account.verified') {
      await markAccountVerified(event.data.token);
    } else if (event.type === 'bank_account.verification_failed') {
      await markAccountFailed(event.data.token);
    }
    res.status(204).end();
  } catch (err) {
    next(err);
  }
}

payoutsRouter.post('/webhooks/payrail', express.raw({ type: 'application/json', limit: '256kb' }), payrailWebhookHandler);
payoutsRouter.use(requireAuth, express.json({ limit: '32kb' }));
payoutsRouter.get('/accounts', listAccountsHandler);
payoutsRouter.post('/accounts', linkAccountHandler);
payoutsRouter.get('/accounts/:accountId', getAccountHandler);
payoutsRouter.delete('/accounts/:accountId', removePayoutAccountHandler);
payoutsRouter.put('/accounts/:accountId/webhook', registerWebhookHandler);
payoutsRouter.post('/onboarding-session', onboardingSessionHandler);
