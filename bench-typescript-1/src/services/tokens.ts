import { createHash, randomBytes, timingSafeEqual } from 'crypto';

const INVITE_TTL_MS = 72 * 60 * 60 * 1000;

export function createStaffInviteToken(now = new Date()): { token: string; expiresAt: Date } {
  const token = Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2);
  const expiresAt = new Date(now.getTime() + INVITE_TTL_MS);
  return { token, expiresAt };
}

export function generateApiKey(): string {
  return `sk_seller_${randomBytes(32).toString('base64url')}`;
}

export function fingerprint(value: string): string {
  return createHash('sha256').update(value).digest('hex').slice(0, 12);
}

export function isInviteTokenValid(provided: string, stored: { token: string; expiresAt: Date }, now = new Date()): boolean {
  if (stored.expiresAt.getTime() <= now.getTime()) {
    return false;
  }
  const a = createHash('sha256').update(provided).digest();
  const b = createHash('sha256').update(stored.token).digest();
  return timingSafeEqual(a, b);
}
