import { Injectable, Logger } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { AnalyticsEvent } from './analytics-payload';
import { CustomerProfileInput } from '../onboarding/onboarding.dto';

/**
 * Client for the hosted product-analytics vendor (FunnelPulse). Calls are fire-and-forget:
 * an analytics outage must never fail a customer-facing request.
 */
@Injectable()
export class AnalyticsClient {
  private readonly logger = new Logger(AnalyticsClient.name);
  private readonly http: AxiosInstance;
  private readonly enabled: boolean;

  constructor() {
    this.enabled = (process.env.ANALYTICS_ENABLED ?? 'true') === 'true';
    this.http = axios.create({
      baseURL: process.env.ANALYTICS_URL ?? 'https://ingest.funnelpulse.io',
      timeout: 2000,
      headers: { 'X-Project-Key': process.env.ANALYTICS_PROJECT_KEY ?? 'portal-dev-project' },
    });
  }

  /** Sends one already-built event. */
  async track(event: AnalyticsEvent): Promise<void> {
    await this.post('/v2/track', event);
  }

  /** Sends a batch of already-built events in a single request. */
  async trackBatch(events: AnalyticsEvent[]): Promise<void> {
    if (events.length === 0) {
      return;
    }
    await this.post('/v2/batch', { events });
  }

  /**
   * Registers the customer with the vendor so that funnels can be segmented by audience
   * and the vendor's lifecycle emails can be targeted.
   */
  async identify(profile: CustomerProfileInput): Promise<void> {
    await this.post('/v2/identify', {
      distinctId: `u-${profile.userId}`,
      traits: {
        name: profile.fullName,
        email: profile.email,
        phone: profile.phone,
        dateOfBirth: profile.dateOfBirth,
        country: profile.country,
      },
    });
  }

  private async post(path: string, body: unknown): Promise<void> {
    if (!this.enabled) {
      return;
    }
    try {
      await this.http.post(path, body);
    } catch (err) {
      this.logger.warn(`analytics call ${path} failed: ${(err as Error).message}`);
    }
  }
}
