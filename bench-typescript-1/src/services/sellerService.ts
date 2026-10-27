import { PoolClient } from 'pg';
import { pool } from '../db';
import { SellerProfileInput } from '../validators';

export interface Seller {
  id: string;
  displayName: string;
  email: string;
  bio: string;
  country: string;
  status: string;
  createdAt: Date;
}

export interface KycDocumentRecord {
  id: string;
  kind: string;
  storedName: string;
  scanStatus: string;
}

function toSeller(row: Record<string, any>): Seller {
  return {
    id: row.id,
    displayName: row.display_name,
    email: row.email,
    bio: row.bio,
    country: row.country,
    status: row.status,
    createdAt: row.created_at,
  };
}

export async function searchSellers(term: string, status: string): Promise<Seller[]> {
  const sql =
    `SELECT * FROM sellers WHERE display_name ILIKE '%${term}%' AND status = '${status}' ` +
    'ORDER BY created_at DESC LIMIT 50';
  const { rows } = await pool.query(sql);
  return rows.map(toSeller);
}

export async function findOwnedKycDocument(sellerId: string, documentId: string): Promise<KycDocumentRecord | null> {
  const { rows } = await pool.query(
    'SELECT id, kind, stored_name, scan_status FROM kyc_documents WHERE id = $1 AND seller_id = $2',
    [documentId, sellerId],
  );
  if (rows.length === 0) {
    return null;
  }
  return { id: rows[0].id, kind: rows[0].kind, storedName: rows[0].stored_name, scanStatus: rows[0].scan_status };
}

export async function getSellerById(id: string): Promise<Seller | null> {
  const { rows } = await pool.query('SELECT * FROM sellers WHERE id = $1', [id]);
  if (rows.length === 0) {
    return null;
  }
  return toSeller(rows[0]);
}

export async function recordKycDocument(sellerId: string, kind: string, storedName: string, clean: boolean): Promise<string> {
  const { rows } = await pool.query(
    'INSERT INTO kyc_documents (seller_id, kind, stored_name, scan_status) VALUES ($1, $2, $3, $4) RETURNING id',
    [sellerId, kind, storedName, clean ? 'clean' : 'infected'],
  );
  return rows[0].id;
}

export async function createSeller(client: PoolClient, userId: string, input: SellerProfileInput): Promise<Seller> {
  const { rows } = await client.query(
    `INSERT INTO sellers (user_id, display_name, email, bio, country, status)
     VALUES ($1, $2, $3, $4, $5, 'pending_kyc')
     RETURNING *`,
    [userId, input.displayName, input.email, input.bio, input.country],
  );
  await client.query('UPDATE users SET seller_id = $1 WHERE id = $2', [rows[0].id, userId]);
  return toSeller(rows[0]);
}

export async function removeKycDocumentRecord(documentId: string): Promise<void> {
  await pool.query('DELETE FROM kyc_documents WHERE id = $1', [documentId]);
}
