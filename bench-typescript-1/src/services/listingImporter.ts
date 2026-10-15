import yaml from 'js-yaml';
import serialize from 'node-serialize';

export interface FeedItem {
  title: string;
  description: string;
  priceCents: number;
}

export interface ListingDraft {
  title: string;
  description: string;
  priceCents: number;
  step: number;
}

const MAX_FEED_ITEMS = 500;

export async function fetchSellerFeed(feedUrl: string): Promise<string> {
  const response = await fetch(feedUrl, {
    headers: { Accept: 'application/x-yaml, text/yaml, text/plain' },
    signal: AbortSignal.timeout(10_000),
  });
  if (!response.ok) {
    throw new Error(`feed request failed with status ${response.status}`);
  }
  return response.text();
}

function toFeedItem(entry: Record<string, unknown>): FeedItem | null {
  const title = typeof entry.title === 'string' ? entry.title.trim() : '';
  const priceCents = Number(entry.price_cents);
  if (!title || !Number.isInteger(priceCents) || priceCents < 0) {
    return null;
  }
  return { title, description: typeof entry.description === 'string' ? entry.description : '', priceCents };
}

export function parseFeedDocument(text: string): FeedItem[] {
  const document = yaml.load(text) as { listings?: unknown } | null | undefined;
  if (!document || !Array.isArray(document.listings)) {
    throw new Error('feed document must contain a listings array');
  }
  return document.listings
    .slice(0, MAX_FEED_ITEMS)
    .map((entry) => (entry && typeof entry === 'object' ? toFeedItem(entry as Record<string, unknown>) : null))
    .filter((item): item is FeedItem => item !== null);
}

export function encodeListingDraft(draft: ListingDraft): string {
  const serialized = serialize.serialize(draft);
  return Buffer.from(serialized, 'utf8').toString('base64');
}

export function restoreListingDraft(encoded: string): ListingDraft {
  const raw = Buffer.from(encoded, 'base64').toString('utf8');
  const draft = serialize.unserialize(raw) as Partial<ListingDraft>;
  return {
    title: String(draft.title ?? ''),
    description: String(draft.description ?? ''),
    priceCents: Number(draft.priceCents ?? 0),
    step: Number(draft.step ?? 1),
  };
}
