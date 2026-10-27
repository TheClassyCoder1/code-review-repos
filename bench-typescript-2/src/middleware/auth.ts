import { NextFunction, Request, Response } from 'express';
import { pool } from '../db';
import { apiKeyPrefix, hashApiKey } from '../crypto';
import { logger } from '../logger';

interface ApiKeyRow {
  account_id: string;
  account_name: string;
  key_hash: string;
}

function bearerToken(req: Request): string | null {
  const header = req.header('authorization') ?? '';
  const [scheme, token] = header.split(' ');
  return scheme === 'Bearer' && token ? token : null;
}

export async function requireApiKey(req: Request, res: Response, next: NextFunction): Promise<void> {
  const provided = bearerToken(req);
  if (!provided) {
    res.status(401).json({ error: 'missing api key' });
    return;
  }
  const { rows } = await pool.query<ApiKeyRow>(
    `SELECT k.account_id, a.name AS account_name, k.key_hash
       FROM api_keys k JOIN accounts a ON a.id = k.account_id
      WHERE k.prefix = $1 AND k.revoked_at IS NULL`,
    [apiKeyPrefix(provided)],
  );
  const record = rows[0];
  if (!record || record.key_hash !== hashApiKey(provided)) {
    logger.info({ prefix: apiKeyPrefix(provided) }, 'api key rejected');
    res.status(401).json({ error: 'invalid api key' });
    return;
  }
  req.account = { id: record.account_id, name: record.account_name };
  next();
}
