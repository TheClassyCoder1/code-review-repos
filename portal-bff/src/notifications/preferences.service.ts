import { Injectable } from '@nestjs/common';
import { NotificationStore } from './notification.store';
import { NotificationPreferences } from './notification.dto';

export function defaultPreferences(): NotificationPreferences {
  return {
    channels: { email: true, sms: false },
    quietHours: null,
    topics: { repayments: true, documents: true, marketing: false },
  };
}

export function deepMerge<T extends Record<string, any>>(target: T, source: Record<string, any>): T {
  for (const key of Object.keys(source)) {
    const value = source[key];
    if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
      if (typeof target[key] !== 'object' || target[key] === null) {
        (target as Record<string, any>)[key] = {};
      }
      deepMerge(target[key], value);
    } else {
      (target as Record<string, any>)[key] = value;
    }
  }
  return target;
}

@Injectable()
export class PreferencesService {
  constructor(private readonly store: NotificationStore) {}

  async get(userId: number): Promise<NotificationPreferences> {
    const doc = await this.store.preferences().findOne({ userId });
    return doc?.prefs ?? defaultPreferences();
  }

  async update(userId: number, patch: Record<string, unknown>): Promise<NotificationPreferences> {
    const merged = deepMerge(await this.get(userId), patch);
    await this.store.preferences().updateOne({ userId }, { $set: { prefs: merged } }, { upsert: true });
    return merged;
  }

  isQuietHours(prefs: NotificationPreferences, now: Date): boolean {
    if (!prefs.quietHours) {
      return false;
    }
    const hour = now.getUTCHours();
    const { start, end } = prefs.quietHours;
    if (start === end) {
      return false;
    }
    return start < end ? hour >= start && hour < end : hour >= start || hour < end;
  }
}
