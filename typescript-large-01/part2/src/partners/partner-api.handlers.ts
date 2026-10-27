import { ClientCredentialStore } from '../api-keys/api-key.store';
import { findPartner } from '../config/partners';
import { HttpError, requireString, sendJson } from '../http/respond';
import { Middleware, RequestContext } from '../http/router';
import { QuotaStore } from '../quotas/quota-store';
import { log } from '../support/log';
import { WebhookRegistry } from '../webhooks/webhook-registry';

/**
 * Basic-auth over client id + client secret for dealer/broker integrations.
 * The authenticated partner id is placed in ctx.userId as `partner:<id>`.
 */
export function authenticatePartner(credentials: ClientCredentialStore, quotas: QuotaStore): Middleware {
  return async (ctx, next) => {
    const header = ctx.req.headers.authorization ?? '';
    const decoded = header.startsWith('Basic ') ? Buffer.from(header.slice(6), 'base64').toString('utf8') : '';
    const sep = decoded.indexOf(':');
    const clientId = sep > 0 ? decoded.slice(0, sep) : '';
    const partnerId = clientId ? credentials.authenticate(clientId, decoded.slice(sep + 1)) : null;
    if (!partnerId) {
      throw new HttpError(401, 'invalid client credentials');
    }
    if (!(await quotas.consume(clientId))) {
      throw new HttpError(429, 'daily request quota exhausted');
    }
    ctx.userId = `partner:${partnerId}`;
    await next();
  };
}

export class PartnerApiHandlers {
  constructor(
    private readonly credentials: ClientCredentialStore,
    private readonly webhooks: WebhookRegistry,
  ) {}

  /** Operator endpoint: mint or rotate a partner's client secret. */
  rotateCredential = async (ctx: RequestContext): Promise<void> => {
    const partner = findPartner(ctx.params.partnerId);
    if (!partner) throw new HttpError(404, 'unknown partner');
    const clientId = `${partner.id}-api`;
    const secret = this.credentials.rotate(clientId, partner.id);
    log.info('partner credential rotated', { partnerId: partner.id, clientId });
    sendJson(ctx.res, 201, { clientId, clientSecret: secret });
  };

  subscribe = async (ctx: RequestContext): Promise<void> => {
    const callbackUrl = requireString(ctx.body, 'callbackUrl');
    const events = (ctx.body as Record<string, unknown>).events;
    const partnerId = (ctx.userId ?? '').replace(/^partner:/, '');
    const sub = this.webhooks.register(partnerId, callbackUrl, Array.isArray(events) ? events.map(String) : []);
    log.info('webhook subscription created', { partnerId, subscriptionId: sub.id });
    sendJson(ctx.res, 201, { id: sub.id, events: sub.events, signingSecret: sub.signingSecret });
  };
}
