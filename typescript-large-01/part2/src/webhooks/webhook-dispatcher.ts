import { createHmac, randomUUID } from 'crypto';
import { errorMessage, log } from '../support/log';
import { WebhookEvent, WebhookRegistry, WebhookSubscription } from './webhook-registry';

const DELIVERY_TIMEOUT_MS = 5000;

/**
 * Fans out portal events to partner webhooks. Each delivery is signed with the
 * subscription's secret so the partner can authenticate it.
 */
export class WebhookDispatcher {
  constructor(private readonly registry: WebhookRegistry) {}

  async publish(event: WebhookEvent, data: Record<string, unknown>): Promise<boolean[]> {
    const deliveryId = randomUUID();
    const body = JSON.stringify({ id: deliveryId, event, occurredAt: new Date().toISOString(), data });
    return Promise.all(this.registry.forEvent(event).map((sub) => this.deliver(sub, deliveryId, body)));
  }

  private async deliver(sub: WebhookSubscription, deliveryId: string, body: string): Promise<boolean> {
    const signature = createHmac('sha256', sub.signingSecret).update(body).digest('hex');
    try {
      const res = await fetch(sub.callbackUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Portal-Delivery': deliveryId, 'X-Portal-Signature': `sha256=${signature}` },
        body,
        redirect: 'manual',
        signal: AbortSignal.timeout(DELIVERY_TIMEOUT_MS),
      });
      if (!res.ok) log.warn('webhook delivery rejected', { subscriptionId: sub.id, status: res.status });
      return res.ok;
    } catch (err) {
      log.warn('webhook delivery failed', { subscriptionId: sub.id, error: errorMessage(err) });
      return false;
    }
  }
}
