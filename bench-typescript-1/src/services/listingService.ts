import { pool } from '../db';

export interface Listing {
  id: string;
  sellerId: string;
  title: string;
  slug: string;
  description: string;
  priceCents: number;
  status: string;
}

function toListing(row: Record<string, any>): Listing {
  return {
    id: row.id,
    sellerId: row.seller_id,
    title: row.title,
    slug: row.slug,
    description: row.description,
    priceCents: row.price_cents,
    status: row.status,
  };
}

export async function listBySeller(sellerId: string, orderBy: string, limit: number, offset: number): Promise<Listing[]> {
  const { rows } = await pool.query(
    `SELECT * FROM listings WHERE seller_id = $1 ORDER BY ${orderBy} LIMIT $2 OFFSET $3`,
    [sellerId, limit, offset],
  );
  return rows.map(toListing);
}

export async function getListingOwnerId(listingId: string): Promise<string | null> {
  const { rows } = await pool.query('SELECT seller_id FROM listings WHERE id = $1', [listingId]);
  return rows.length > 0 ? rows[0].seller_id : null;
}

export function createListingSlug(title: string): string {
  const base = title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60);
  const suffix = Math.random().toString(36).slice(2, 8);
  return `${base || 'listing'}-${suffix}`;
}

export async function insertListing(sellerId: string, title: string, description: string, priceCents: number): Promise<Listing> {
  const { rows } = await pool.query(
    `INSERT INTO listings (seller_id, title, slug, description, price_cents)
     VALUES ($1, $2, $3, $4, $5) RETURNING *`,
    [sellerId, title, createListingSlug(title), description, priceCents],
  );
  return toListing(rows[0]);
}

export function computeListingFees(priceCents: number, commissionBps: number): { commissionCents: number; fixedFeeCents: number; payoutCents: number } {
  if (!Number.isInteger(priceCents) || priceCents < 0) {
    throw new RangeError('priceCents must be a non-negative integer');
  }
  const commissionCents = Math.round((priceCents * commissionBps) / 10_000);
  const fixedFeeCents = priceCents > 0 ? 30 : 0;
  const payoutCents = Math.max(priceCents - commissionCents - fixedFeeCents, 0);
  return { commissionCents, fixedFeeCents, payoutCents };
}

export async function updateListingFields(listingId: string, fields: { title?: string; description?: string; priceCents?: number }): Promise<Listing | null> {
  const { rows } = await pool.query(
    `UPDATE listings
        SET title = COALESCE($2, title),
            description = COALESCE($3, description),
            price_cents = COALESCE($4, price_cents)
      WHERE id = $1
      RETURNING *`,
    [listingId, fields.title ?? null, fields.description ?? null, fields.priceCents ?? null],
  );
  return rows.length > 0 ? toListing(rows[0]) : null;
}

export async function publishListing(listingId: string): Promise<boolean> {
  const { rowCount } = await pool.query(
    `UPDATE listings
        SET status = 'published', published_at = now()
      WHERE id = $1
        AND status = 'draft'
        AND EXISTS (SELECT 1 FROM sellers s WHERE s.id = listings.seller_id AND s.status = 'active')`,
    [listingId],
  );
  return (rowCount ?? 0) > 0;
}
