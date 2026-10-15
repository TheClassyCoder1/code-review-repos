import { EventEmitter } from 'events';
import { logger } from './logger';

const bus = new EventEmitter();

export type BillingEvent = 'usage.recorded' | 'invoice.finalized' | 'subscription.renewed';

export function publish(name: BillingEvent, payload: Record<string, unknown>): void {
  bus.emit(name, payload);
}

export function subscribe(name: BillingEvent, handler: (payload: Record<string, unknown>) => void): void {
  bus.on(name, (payload) => {
    try {
      handler(payload);
    } catch (err) {
      logger.error({ err, event: name }, 'event handler failed');
    }
  });
}
