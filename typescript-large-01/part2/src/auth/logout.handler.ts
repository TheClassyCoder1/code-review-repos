import { sendRedirect } from '../http/respond';
import { RequestContext } from '../http/router';
import { log } from '../support/log';
import { clearSessionCookie } from './authenticate.middleware';

/**
 * Ends the portal session. Partner front-ends (and the main lending frontend)
 * pass `continue` so the borrower lands back on the page they came from.
 */
export class LogoutHandler {
  constructor(private readonly homeUrl: string) {}

  logout = async (ctx: RequestContext): Promise<void> => {
    ctx.res.setHeader('Set-Cookie', clearSessionCookie());
    log.info('portal logout', { userId: ctx.userId });
    const target = ctx.query.get('continue') ?? this.homeUrl;
    sendRedirect(ctx.res, target);
  };
}
