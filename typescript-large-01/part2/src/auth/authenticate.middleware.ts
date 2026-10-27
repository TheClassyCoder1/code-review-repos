import { IncomingMessage } from 'http';
import { HttpError } from '../http/respond';
import { Middleware } from '../http/router';
import { decodeSession, encodeSession, SESSION_TTL_MS, SessionState } from '../session/session-codec';
import { signBlob, verifySignedBlob } from '../session/signed-blob';

export const SESSION_COOKIE = 'portal_session';

function readCookie(req: IncomingMessage, name: string): string | null {
  for (const part of (req.headers.cookie ?? '').split(';')) {
    const eq = part.indexOf('=');
    if (eq > 0 && part.slice(0, eq).trim() === name) {
      return decodeURIComponent(part.slice(eq + 1).trim());
    }
  }
  return null;
}

export function issueSessionCookie(state: SessionState, secret: string, secure: boolean): string {
  const token = signBlob(encodeSession(state), secret);
  const maxAge = Math.floor(SESSION_TTL_MS / 1000);
  return `${SESSION_COOKIE}=${encodeURIComponent(token)}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${maxAge}${secure ? '; Secure' : ''}`;
}

export function clearSessionCookie(): string {
  return `${SESSION_COOKIE}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0`;
}

/** Resolves the borrower from the signed session cookie; rejects the request otherwise. */
export function authenticate(secret: string): Middleware {
  return async (ctx, next) => {
    const token = readCookie(ctx.req, SESSION_COOKIE);
    const verified = token ? verifySignedBlob(token, secret) : null;
    const session = verified ? decodeSession(verified) : null;
    if (!session) {
      throw new HttpError(401, 'authentication required');
    }
    ctx.userId = session.userId;
    await next();
  };
}
