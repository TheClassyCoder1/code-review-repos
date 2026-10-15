import { Injectable, OnModuleDestroy } from '@nestjs/common';
import { Collection, Db, MongoClient } from 'mongodb';
import { notifyConfig } from '../config/notify.config';
import {
  DeliveryRecord,
  NotificationPreferences,
  PartnerRecord,
  WebhookSubscription,
} from './notification.dto';

@Injectable()
export class NotificationStore implements OnModuleDestroy {
  private readonly client = new MongoClient(notifyConfig.mongoUrl);
  private readonly db: Db = this.client.db('portal');

  subscriptions(): Collection<WebhookSubscription> {
    return this.db.collection<WebhookSubscription>('webhook_subscriptions');
  }

  deliveries(): Collection<DeliveryRecord> {
    return this.db.collection<DeliveryRecord>('webhook_deliveries');
  }

  partners(): Collection<PartnerRecord> {
    return this.db.collection<PartnerRecord>('partners');
  }

  preferences(): Collection<{ userId: number; prefs: NotificationPreferences }> {
    return this.db.collection('notification_preferences');
  }

  async subscriptionsForPartner(partnerId: string): Promise<WebhookSubscription[]> {
    return this.subscriptions()
      .find({ partnerId })
      .sort({ createdAt: -1 })
      .limit(100)
      .toArray();
  }

  async onModuleDestroy(): Promise<void> {
    await this.client.close();
  }
}
