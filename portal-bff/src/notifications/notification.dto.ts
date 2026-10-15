import { ObjectId } from 'mongodb';
import { z } from 'zod';

export const PARTNER_EVENTS = ['loan.approved', 'loan.rejected', 'document.ready'] as const;

export const RegisterWebhookSchema = z.object({
  event: z.enum(PARTNER_EVENTS),
  callbackUrl: z.string().url().max(2048),
});
export type RegisterWebhookInput = z.infer<typeof RegisterWebhookSchema>;

export const DocumentKeySchema = z.object({
  docKey: z.string().regex(/^[a-z0-9-]{8,64}\.pdf$/),
});

export interface PartnerEvent {
  type: (typeof PARTNER_EVENTS)[number];
  loanId: number;
  occurredAt: string;
}

export interface WebhookSubscription {
  _id?: ObjectId;
  partnerId: string;
  event: string;
  callbackUrl: string;
  createdAt: Date;
}

export interface DeliveryRecord {
  subscriptionId?: ObjectId;
  partnerId: string;
  event: string;
  status: 'DELIVERED' | 'FAILED';
  at: Date;
}

export interface PartnerRecord {
  _id?: ObjectId;
  name: string;
  clientId: string;
  clientSecret: string;
  apiKeys: string[];
}

export interface NotificationPreferences {
  channels: { email: boolean; sms: boolean };
  quietHours: { start: number; end: number } | null;
  topics: Record<string, boolean>;
}

export interface PartnerIdentity {
  id: string;
  name: string;
}
