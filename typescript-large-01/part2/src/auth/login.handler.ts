import { AppConfig } from '../config/app-config';
import { HttpError, requireString, sendRedirect } from '../http/respond';
import { RequestContext } from '../http/router';
import { log } from '../support/log';
import { issueSessionCookie } from './authenticate.middleware';
import { CredentialVerifier } from './credential-verifier';
import { resolveReturnPath } from './return-path';

export class LoginHandler {
  constructor(
    private readonly config: AppConfig,
    private readonly credentials: CredentialVerifier,
  ) {}

  login = async (ctx: RequestContext): Promise<void> => {
    const login = requireString(ctx.body, 'login');
    const password = requireString(ctx.body, 'password');
    const userId = await this.credentials.verify(login, password);
    if (!userId) {
      log.warn('portal login rejected');
      throw new HttpError(401, 'invalid login or password');
    }
    const next = resolveReturnPath(ctx.query.get('next'));
    const cookie = issueSessionCookie(
      { userId, issuedAt: Date.now(), roles: ['borrower'] },
      this.config.sessionSecret,
      this.config.production,
    );
    ctx.res.setHeader('Set-Cookie', cookie);
    log.info('portal login', { userId });
    sendRedirect(ctx.res, next, 303);
  };
}
