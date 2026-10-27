import { HttpError } from '../http/respond';
import { Middleware } from '../http/router';

/**
 * Rejects a request whose redirect parameter points anywhere other than one of
 * the configured portal origins. Routes that bounce the browser to a
 * caller-supplied URL must be registered behind this middleware.
 */
export function requireAllowedRedirect(allowedOrigins: readonly string[], param = 'continue'): Middleware {
  const allowed = new Set(allowedOrigins.map((o) => new URL(o).origin));
  return async (ctx, next) => {
    const raw = ctx.query.get(param);
    if (raw !== null) {
      let target: URL;
      try {
        target = new URL(raw);
      } catch {
        throw new HttpError(400, `${param} must be an absolute URL`);
      }
      if (target.protocol !== 'https:' && target.protocol !== 'http:') {
        throw new HttpError(400, `${param} scheme not allowed`);
      }
      if (target.username || target.password || !allowed.has(target.origin)) {
        throw new HttpError(400, `${param} origin not allowed`);
      }
    }
    await next();
  };
}
