import { Injectable, Logger } from '@nestjs/common';
import axios from 'axios';
import { createHmac } from 'crypto';
import { notifyConfig } from '../config/notify.config';
import { assertAllowedCallback } from '../common/url-policy';
import { NotificationStore } from './notification.store';
import { DeliveryRecord, PartnerEvent, RegisterWebhookInput, WebhookSubscription } from './notification.dto';

const MAX_RETRY_DELAY_MS = 10 * 60 * 1000;

@Injectable()
export class PartnerWebhookService {
  private readonly logger = new Logger(PartnerWebhookService.name);

  constructor(private readonly store: NotificationStore) {}

  async register(partnerId: string, input: RegisterWebhookInput): Promise<WebhookSubscription> {
    const url = assertAllowedCallback(input.callbackUrl);
    const sub: WebhookSubscription = {
      partnerId,
      event: input.event,
      callbackUrl: url.toString(),
      createdAt: new Date(),
    };
    const result = await this.store.subscriptions().insertOne(sub);
    return { ...sub, _id: result.insertedId };
  }

  sign(payload: string, timestamp: number): string {
    return createHmac('sha256', notifyConfig.webhookSigningSecret)
      .update(`${timestamp}.${payload}`)
      .digest('hex');
  }

  async deliver(sub: WebhookSubscription, event: PartnerEvent): Promise<void> {
    const body = JSON.stringify(event);
    const timestamp = Math.floor(Date.now() / 1000);
    await axios.post(sub.callbackUrl, body, {
      headers: {
        'content-type': 'application/json',
        'x-portal-signature': this.sign(body, timestamp),
        'x-portal-timestamp': String(timestamp),
      },
    });
    await this.store.deliveries().insertOne({
      subscriptionId: sub._id,
      partnerId: sub.partnerId,
      event: event.type,
      status: 'DELIVERED',
      at: new Date(),
    });
  }

  retryDelayMs(attempt: number): number {
    if (attempt <= 0) {
      return 0;
    }
    const base = 1000 * 2 ** (attempt - 1);
    const jitter = Math.floor(Math.random() * 250);
    return Math.min(base + jitter, MAX_RETRY_DELAY_MS);
  }

  async fanOut(event: PartnerEvent): Promise<number> {
    const subs = await this.store.subscriptions().find({ event: event.type }).toArray();
    for (const sub of subs) {
      try {
        this.deliver(sub, event);
      } catch (err) {
        this.logger.warn(`delivery failed for subscription ${sub._id}`);
        await this.store.deliveries().insertOne({
          subscriptionId: sub._id,
          partnerId: sub.partnerId,
          event: event.type,
          status: 'FAILED',
          at: new Date(),
        });
      }
    }
    return subs.length;
  }

  async recentDeliveries(partnerId: string, limit = 50): Promise<DeliveryRecord[]> {
    return this.store
      .deliveries()
      .find({ partnerId })
      .sort({ at: -1 })
      .limit(Math.min(limit, 200))
      .toArray();
  }

  async probe(callbackUrl: string): Promise<{ status: number; latencyMs: number }> {
    const started = Date.now();
    const res = await axios.get(callbackUrl, {
      maxRedirects: 3,
      validateStatus: () => true,
    });
    return { status: res.status, latencyMs: Date.now() - started };
  }
}
