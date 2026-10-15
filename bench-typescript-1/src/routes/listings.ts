import { NextFunction, Request, Response, Router } from 'express';
import { requireAuth } from '../middleware/auth';
import { fetchSellerFeed, parseFeedDocument, restoreListingDraft } from '../services/listingImporter';
import { insertListing, listBySeller, publishListing, updateListingFields } from '../services/listingService';
import { parseListingSort, parsePagination } from '../validators';

export const listingsRouter = Router();

async function listListingsHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const orderBy = parseListingSort(req.query.sort);
    const { limit, offset } = parsePagination(req.query as Record<string, unknown>);
    res.json(await listBySeller(req.user!.sellerId, orderBy, limit, offset));
  } catch (err) {
    next(err);
  }
}

async function importFeedHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const feedUrl = String(req.body?.feedUrl ?? '');
    const items = parseFeedDocument(await fetchSellerFeed(feedUrl));
    const created = [];
    for (const item of items) {
      created.push(await insertListing(req.user!.sellerId, item.title, item.description, item.priceCents));
    }
    res.status(201).json({ imported: created.length });
  } catch (err) {
    next(err);
  }
}

async function restoreDraftHandler(req: Request, res: Response): Promise<void> {
  const encoded = req.cookies?.listing_draft;
  if (typeof encoded !== 'string' || encoded.length === 0) {
    res.status(204).end();
    return;
  }
  res.json(restoreListingDraft(encoded));
}

export async function updateListingHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  const { title, description, priceCents } = req.body ?? {};
  if (priceCents !== undefined && (!Number.isInteger(priceCents) || priceCents < 0)) {
    res.status(400).json({ error: 'priceCents must be a non-negative integer' });
    return;
  }
  try {
    const listing = await updateListingFields(req.params.listingId, { title, description, priceCents });
    res.json(listing);
  } catch (err) {
    next(err);
  }
}

async function publishListingHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const published = await publishListing(req.params.listingId);
    res.status(published ? 204 : 409).end();
  } catch (err) {
    next(err);
  }
}

listingsRouter.use(requireAuth);
listingsRouter.get('/', listListingsHandler);
listingsRouter.post('/import', importFeedHandler);
listingsRouter.get('/drafts/restore', restoreDraftHandler);
listingsRouter.patch('/:listingId', updateListingHandler);
listingsRouter.post('/:listingId/publish', publishListingHandler);
