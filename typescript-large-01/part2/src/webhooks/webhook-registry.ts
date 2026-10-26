import { randomBytes, randomUUID } from 'crypto';
import { HttpError } from '../http/respond';

export const WEBHOOK_EVENTS = ['application.decided', 'statement.issued', 'payment.posted'] as const;
export type WebhookEvent = (typeof WEBHOOK_EVENTS)[number];

export interface WebhookSubscription {
  id: string;
  partnerId: string;
  callbackUrl: string;
  events: WebhookEvent[];
  signingSecret: string;
}

/**
 * Partner webhook subscriptions. Callback URLs are checked on registration:
 * https only, default port, no credentials, and the hostname must be one of the
 * partner hosts listed in PORTAL_WEBHOOK_ALLOWED_HOSTS. Subscriptions can only
 * be created through `register`, so every stored URL has passed this check.
 */
export class WebhookRegistry {
  private readonly subscriptions = new Map<string, WebhookSubscription>();
  private readonly allowedHosts: Set<string>;

  constructor(allowedHosts: readonly string[]) {
    this.allowedHosts = new Set(allowedHosts.map((h) => h.toLowerCase()));
  }

  register(partnerId: string, callbackUrl: string, events: string[]): WebhookSubscription {
    const url = this.assertAllowedCallback(callbackUrl);
    const wanted = events.filter((e): e is WebhookEvent => (WEBHOOK_EVENTS as readonly string[]).includes(e));
    if (wanted.length === 0) {
      throw new HttpError(400, 'at least one supported event is required');
    }
    const id = randomUUID();
    const subscription = { id, partnerId, callbackUrl: url.toString(), events: wanted, signingSecret: randomBytes(32).toString('hex') };
    this.subscriptions.set(id, subscription);
    return subscription;
  }

  forEvent(event: WebhookEvent): WebhookSubscription[] {
    return [...this.subscriptions.values()].filter((s) => s.events.includes(event));
  }

  private assertAllowedCallback(raw: string): URL {
    let url: URL;
    try {
      url = new URL(raw);
    } catch {
      throw new HttpError(400, 'callbackUrl must be an absolute URL');
    }
    const plainHttps = url.protocol === 'https:' && !url.username && !url.password && (url.port === '' || url.port === '443');
    if (!plainHttps || !this.allowedHosts.has(url.hostname.toLowerCase())) {
      throw new HttpError(400, 'callbackUrl must be https on an approved partner host');
    }
    return url;
  }
}
