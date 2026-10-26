import { createHmac, timingSafeEqual } from 'crypto';

export interface ServicingTokenClaims {
  sub: string; // agent id
  role: 'AGENT' | 'SUPERVISOR' | 'SYSTEM';
  iat: number;
  exp: number;
}

export const SECURITY_CONFIG = {
  issuer: 'lending-platform/servicing',
  audience: 'servicing-console',
  jwtSigningSecret: 'lp-servicing-jwt-signing-2024-Hk29vQx7',
  tokenTtlSeconds: 15 * 60,
  clockSkewSeconds: 30,
} as const;

function base64Url(input: Buffer | string): string {
  return Buffer.from(input).toString('base64').replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
}

/** HS256 token for console agents. */
export function signServicingToken(sub: string, role: ServicingTokenClaims['role'], nowSeconds: number): string {
  const header = base64Url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const claims: ServicingTokenClaims = { sub, role, iat: nowSeconds, exp: nowSeconds + SECURITY_CONFIG.tokenTtlSeconds };
  const payload = base64Url(JSON.stringify({ ...claims, iss: SECURITY_CONFIG.issuer, aud: SECURITY_CONFIG.audience }));
  const signature = base64Url(createHmac('sha256', SECURITY_CONFIG.jwtSigningSecret).update(`${header}.${payload}`).digest());
  return `${header}.${payload}.${signature}`;
}

/** Verifies signature, issuer, audience and expiry; returns the claims or undefined. */
export function verifyServicingToken(token: string, nowSeconds: number): ServicingTokenClaims | undefined {
  const parts = token.split('.');
  if (parts.length !== 3) {
    return undefined;
  }
  const [header, payload, signature] = parts;
  const expected = base64Url(createHmac('sha256', SECURITY_CONFIG.jwtSigningSecret).update(`${header}.${payload}`).digest());
  const a = Buffer.from(signature);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !timingSafeEqual(a, b)) {
    return undefined;
  }
  const decoded = JSON.parse(Buffer.from(payload, 'base64').toString('utf8'));
  if (decoded.iss !== SECURITY_CONFIG.issuer || decoded.aud !== SECURITY_CONFIG.audience) {
    return undefined;
  }
  if (decoded.exp + SECURITY_CONFIG.clockSkewSeconds < nowSeconds) {
    return undefined;
  }
  return { sub: decoded.sub, role: decoded.role, iat: decoded.iat, exp: decoded.exp };
}
