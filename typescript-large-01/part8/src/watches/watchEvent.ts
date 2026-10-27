import { createHash } from 'crypto';

export interface WatchUser {
  id: number;
  email: string;
}

export interface Watch {
  id: number;
  secret: string;
  isActive: boolean;
  eventType: string;
  contentType: string | null;
  objectId: number | null;
  user: WatchUser | null;
  email: string | null;
}

export interface CreateWatch {
  secret: string;
  isActive: boolean;
  eventType: string;
  contentType?: string;
  objectId?: number;
  user?: WatchUser;
  email?: string;
}

export interface WatchStore {
  findMatching(
    userOrEmail: WatchUser | string,
    eventType: string,
    contentType: string | null,
    objectId: number | null,
    filters: Record<string, string | number>,
  ): Promise<Watch[]>;
  createWatch(fields: CreateWatch): Promise<Watch>;
  createFilter(watchId: number, name: string, value: number): Promise<void>;
  deleteWatch(watchId: number): Promise<void>;
}

export interface ActivationMessage {
  to: string;
  subject: string;
  body: string;
}

export interface Mailer {
  send(message: ActivationMessage): Promise<void>;
}

export class SmtpError extends Error {
  constructor(readonly recipients: string[], message: string) {
    super(message);
  }
}

export class ActivationRequestFailed extends Error {
  constructor(readonly recipients: string[]) {
    super('activation request failed');
  }
}

export interface WatchSettings {
  confirmAnonymousWatches: boolean;
  siteUrl: string;
}

/** Reduces a filter value to an unsigned 32-bit number so it fits an integer column. */
export function hashToUnsigned(value: string | number): number {
  if (typeof value === 'number') {
    return value >>> 0;
  }
  return createHash('md5').update(value).digest().readUInt32BE(0);
}

/**
 * An event users or anonymous email addresses can subscribe to. Subclasses set
 * the static eventType / contentType.
 */
export class WatchEvent {
  static eventType = 'generic';
  static contentType: string | null = null;
  static store: WatchStore;
  static mailer: Mailer;
  static settings: WatchSettings = { confirmAnonymousWatches: true, siteUrl: 'http://localhost' };

  /**
   * Start notifying the given user or email address when this event occurs and
   * meets the criteria given in filters. Returns the created (or the existing
   * matching) watch.
   *
   * Sends an activation email if an anonymous watch is created and anonymous
   * watches must be confirmed. If the activation request fails, throws
   * ActivationRequestFailed.
   */
  static async notify(
    this: typeof WatchEvent,
    userOrEmail: WatchUser | string,
    objectId: number | null = null,
    filters: Record<string, string | number> = {},
  ): Promise<Watch> {
    // A test-for-existence-then-create race exists here, but de-duplication on
    // fire() and deletion of all matches on stopNotifying() nullify its effects.
    let watch: Watch;
    const existing = await this.store.findMatching(userOrEmail, this.eventType, this.contentType, objectId, filters);
    if (existing.length > 0) {
      // Pick 1 if >1 are returned
      watch = existing[0];
    } else {
      const createKwargs: Partial<CreateWatch> = {};
      if (this.contentType) {
        createKwargs.contentType = this.contentType;
      }
      if (typeof userOrEmail === 'string') {
        createKwargs.email = userOrEmail;
      } else {
        createKwargs.user = userOrEmail;
      }
      // Letters that can't be mistaken for other letters or numbers in most
      // fonts, in case people try to type these:
      const distinguishableLetters = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRTUVWXYZ';
      let secret = '';
      for (let x = 0; x < 10; x++) {
        secret += distinguishableLetters.charAt(Math.floor(Math.random() * distinguishableLetters.length));
      }
      // Registered users don't need to confirm, but anonymous users do.
      const isActive = createKwargs.user !== undefined || !this.settings.confirmAnonymousWatches;
      if (objectId) {
        createKwargs.objectId = objectId;
      }
      watch = await this.store.createWatch({ ...createKwargs, secret, isActive, eventType: this.eventType });
      for (const [k, v] of Object.entries(filters)) {
        await this.store.createFilter(watch.id, k, hashToUnsigned(v));
      }
    }
    // Send email for inactive watches.
    if (!watch.isActive) {
      const email = watch.user ? watch.user.email : (watch.email ?? '');
      const message = this.activationEmail(watch, email);
      try {
        await this.mailer.send(message);
      } catch (e) {
        if (e instanceof SmtpError) {
          await this.store.deleteWatch(watch.id);
          throw new ActivationRequestFailed(e.recipients);
        }
        throw e;
      }
    }
    return watch;
  }

  static activationEmail(this: typeof WatchEvent, watch: Watch, email: string): ActivationMessage {
    const link = `${this.settings.siteUrl}/watches/${watch.id}/activate/${watch.secret}`;
    return {
      to: email,
      subject: 'Please confirm your subscription',
      body: `Confirm you want updates by visiting ${link}`,
    };
  }
}
