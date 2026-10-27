import { createHmac, timingSafeEqual } from 'crypto';

declare const verifiedBrand: unique symbol;

/** Bytes whose HMAC has been checked against the session secret. */
export type VerifiedBlob = Buffer & { readonly [verifiedBrand]: true };

function mac(payload: Buffer, secret: string): Buffer {
  return createHmac('sha256', secret).update(payload).digest();
}

export function signBlob(payload: Buffer, secret: string): string {
  return `${payload.toString('base64url')}.${mac(payload, secret).toString('base64url')}`;
}

/**
 * Session cookies are minted by this service only. A token whose MAC does not
 * match is rejected here, so nothing a client crafted ever reaches the session decoder.
 */
export function verifySignedBlob(token: string, secret: string): VerifiedBlob | null {
  const dot = token.lastIndexOf('.');
  if (dot <= 0 || dot === token.length - 1) return null;
  const payload = Buffer.from(token.slice(0, dot), 'base64url');
  const presented = Buffer.from(token.slice(dot + 1), 'base64url');
  const expected = mac(payload, secret);
  if (presented.length !== expected.length || !timingSafeEqual(presented, expected)) {
    return null;
  }
  return payload as VerifiedBlob;
}
