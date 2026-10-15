import { z } from 'zod';
import { config } from './config';

const LISTING_SORT_COLUMNS = {
  newest: 'created_at DESC',
  oldest: 'created_at ASC',
  price_asc: 'price_cents ASC',
  price_desc: 'price_cents DESC',
  title: 'title ASC',
} as const;

type ListingSortKey = keyof typeof LISTING_SORT_COLUMNS;

export function parseListingSort(raw: unknown): string {
  if (typeof raw === 'string' && Object.prototype.hasOwnProperty.call(LISTING_SORT_COLUMNS, raw)) {
    return LISTING_SORT_COLUMNS[raw as ListingSortKey];
  }
  return LISTING_SORT_COLUMNS.newest;
}

const STORED_KYC_NAME = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(pdf|png|jpg)$/;

export function isStoredKycName(name: unknown): name is string {
  return typeof name === 'string' && STORED_KYC_NAME.test(name);
}

export function isAllowedWebhookHost(rawUrl: string): boolean {
  let parsed: URL;
  try {
    parsed = new URL(rawUrl);
  } catch {
    return false;
  }
  return parsed.protocol === 'https:' && parsed.port === '' && config.webhookHosts.includes(parsed.hostname);
}

export const sellerProfileSchema = z.object({
  displayName: z.string().regex(/^[A-Za-z0-9 .-]{2,60}$/),
  email: z.string().email(),
  bio: z.string().max(2000).default(''),
  country: z.string().regex(/^[A-Z]{2}$/),
});

export type SellerProfileInput = z.infer<typeof sellerProfileSchema>;

export const payoutAccountSchema = z.object({
  holderName: z.string().min(2).max(120),
  accountNumber: z.string().regex(/^[0-9]{4,17}$/),
  routingNumber: z.string().regex(/^[0-9]{9}$/),
  currency: z.enum(['USD', 'EUR', 'GBP']),
});

export type PayoutAccountInput = z.infer<typeof payoutAccountSchema>;

export function parsePagination(query: Record<string, unknown>): { limit: number; offset: number } {
  const requestedLimit = Number.parseInt(String(query.limit ?? '20'), 10);
  const requestedPage = Number.parseInt(String(query.page ?? '1'), 10);
  const limit = Math.min(Math.max(Number.isNaN(requestedLimit) ? 20 : requestedLimit, 1), 100);
  const page = Math.max(Number.isNaN(requestedPage) ? 1 : requestedPage, 1);
  return { limit, offset: (page - 1) * limit };
}
