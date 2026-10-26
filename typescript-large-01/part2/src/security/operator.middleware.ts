import { createHash, timingSafeEqual } from 'crypto';
import { HttpError } from '../http/respond';
import { Middleware } from '../http/router';

const digest = (value: string): Buffer => createHash('sha256').update(value).digest();

/** Guards back-office routes with the shared operator token from PORTAL_OPERATOR_TOKEN. */
export function requireOperator(operatorToken: string | undefined): Middleware {
  const expected = operatorToken ? digest(operatorToken) : null;
  return async (ctx, next) => {
    const presented = ctx.req.headers['x-operator-token'];
    if (!expected || typeof presented !== 'string' || !timingSafeEqual(digest(presented), expected)) {
      throw new HttpError(403, 'operator access required');
    }
    await next();
  };
}
