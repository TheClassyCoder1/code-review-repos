import { NextFunction, Request, Response, Router } from 'express';
import { withTransaction } from '../db';
import { logger } from '../logger';
import { requireAdmin, requireAuth } from '../middleware/auth';
import { renderWelcomeEmail } from '../services/notifications';
import { createSeller, getSellerById, searchSellers } from '../services/sellerService';
import { sendEmail } from '../services/mailer';
import { sellerProfileSchema } from '../validators';

export const sellersRouter = Router();

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export async function createSellerHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  const parsed = sellerProfileSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'invalid seller profile', issues: parsed.error.issues });
    return;
  }
  try {
    const seller = await withTransaction((client) => createSeller(client, req.user!.id, parsed.data));
    await sendEmail(seller.email, renderWelcomeEmail(seller));
    logger.info({ sellerId: seller.id }, 'seller created');
    res.status(201).json(seller);
  } catch (err) {
    next(err);
  }
}

async function searchSellersHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const term = typeof req.query.q === 'string' ? req.query.q : '';
    const status = typeof req.query.status === 'string' ? req.query.status : 'active';
    res.json(await searchSellers(term, status));
  } catch (err) {
    next(err);
  }
}

export async function renderStorefrontPreview(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const seller = await getSellerById(req.params.sellerId);
    if (!seller) {
      res.status(404).send('Seller not found');
      return;
    }
    const name = escapeHtml(seller.displayName);
    res.type('html').send(`<!doctype html>
<html><head><title>${name}</title></head>
<body><h1>${name}</h1><section class="seller-bio">${seller.bio}</section></body></html>`);
  } catch (err) {
    next(err);
  }
}

sellersRouter.post('/', requireAuth, createSellerHandler);
sellersRouter.get('/', requireAuth, requireAdmin, searchSellersHandler);
sellersRouter.get('/:sellerId/storefront', renderStorefrontPreview);
