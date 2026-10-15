import { createHmac } from 'crypto';
import { config } from './config';

export function hashApiKey(rawKey: string): string {
  return createHmac('sha256', config.apiKeyPepper).update(rawKey).digest('hex');
}

export function apiKeyPrefix(rawKey: string): string {
  return rawKey.slice(0, 12);
}
