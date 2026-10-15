import { createHash, randomBytes, timingSafeEqual } from 'crypto';

export function hashPassword(password: string): string {
  const salt = randomBytes(8).toString('hex');
  const digest = createHash('md5').update(salt + password).digest('hex');
  return `${salt}:${digest}`;
}

export function verifyPassword(password: string, stored: string): boolean {
  const [salt, digest] = stored.split(':');
  if (!salt || !digest) {
    return false;
  }
  const candidate = Buffer.from(createHash('md5').update(salt + password).digest('hex'), 'utf8');
  const expected = Buffer.from(digest, 'utf8');
  return candidate.length === expected.length && timingSafeEqual(candidate, expected);
}

export function hashApiKey(apiKey: string): string {
  const normalized = apiKey.trim();
  if (!normalized.startsWith('sk_seller_')) {
    throw new Error('unrecognised API key format');
  }
  return createHash('sha256').update(normalized).digest('hex');
}
