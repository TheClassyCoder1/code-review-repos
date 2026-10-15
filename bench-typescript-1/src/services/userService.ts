import jwt from 'jsonwebtoken';
import { config } from '../config';
import { pool } from '../db';
import { hashApiKey, hashPassword } from './passwords';
import { createStaffInviteToken, generateApiKey } from './tokens';

export interface UserRecord {
  id: string;
  email: string;
  passwordHash: string;
  role: 'seller' | 'admin';
  sellerId: string | null;
}

export async function findUserByEmail(email: string): Promise<UserRecord | null> {
  const { rows } = await pool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase()]);
  if (rows.length === 0) {
    return null;
  }
  const row = rows[0];
  return { id: row.id, email: row.email, passwordHash: row.password_hash, role: row.role, sellerId: row.seller_id };
}

export async function registerUser(email: string, password: string): Promise<string> {
  const { rows } = await pool.query('INSERT INTO users (email, password_hash) VALUES ($1, $2) RETURNING id', [
    email.toLowerCase(),
    hashPassword(password),
  ]);
  return rows[0].id;
}

export function issueSessionToken(user: UserRecord): string {
  return jwt.sign({ id: user.id, sellerId: user.sellerId, role: user.role }, config.jwtSecret, { expiresIn: '12h' });
}

export async function inviteStaffMember(sellerId: string, email: string): Promise<string> {
  const { token, expiresAt } = createStaffInviteToken();
  await pool.query('INSERT INTO staff_invites (seller_id, email, token, expires_at) VALUES ($1, $2, $3, $4)', [
    sellerId,
    email.toLowerCase(),
    token,
    expiresAt,
  ]);
  return token;
}

export async function createApiKey(sellerId: string): Promise<string> {
  const apiKey = generateApiKey();
  await pool.query('INSERT INTO api_keys (seller_id, key_hash) VALUES ($1, $2)', [sellerId, hashApiKey(apiKey)]);
  return apiKey;
}
