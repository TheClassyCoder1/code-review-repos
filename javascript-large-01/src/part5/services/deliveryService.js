'use strict';

const { normaliseChannelPreferences, selectChannel } = require('../delivery/channelSelector');
const { resolveSendTime } = require('../delivery/deliveryWindow');
const { computeBackoffDelay, classifyDeliveryError, DEFAULT_BACKOFF } = require('../retry/backoffPolicy');

const DEFAULT_WINDOW = Object.freeze({ startHour: 8, endHour: 20, weekdaysOnly: false });

class DeliveryConflictError extends Error {
  constructor(message) {
    super(message);
    this.name = 'DeliveryConflictError';
    this.status = 409;
  }
}

/**
 * Orchestrates statement delivery jobs on top of the outbox. Every dependency
 * is passed in through the constructor so the dispatcher and the HTTP layer
 * can share one instance and tests can substitute a fake clock and store.
 *
 * Log lines carry internal ids only (job, statement, owner); addresses and
 * names never pass through this service.
 */
class DeliveryService {
  constructor({ outbox, channelSettings, logger, clock = () => Date.now(), window = DEFAULT_WINDOW, backoff = DEFAULT_BACKOFF }) {
    if (!outbox) {
      throw new TypeError('DeliveryService requires an outbox');
    }
    this.outbox = outbox;
    this.channelSettings = channelSettings || {};
    this.logger = logger;
    this.clock = clock;
    this.window = window;
    this.backoff = backoff;
  }

  /**
   * Queue a delivery for a statement the caller already owns. A statement can
   * only have one active job at a time; a second request while one is pending
   * or in flight is rejected rather than producing a duplicate notification.
   */
  enqueueStatementDelivery(statement, rawPreferences, quietHours) {
    if (!statement || typeof statement.id !== 'string' || typeof statement.ownerId !== 'string') {
      throw new TypeError('statement with id and ownerId is required');
    }
    const existing = this.outbox.findActiveForStatement(statement.id);
    if (existing) {
      throw new DeliveryConflictError(`statement ${statement.id} already has an active delivery`);
    }

    const preferences = normaliseChannelPreferences(rawPreferences);
    const now = this.clock();
    const notBefore = resolveSendTime(now, this.window, quietHours || null);

    const job = this.outbox.insert({
      statementId: statement.id,
      ownerId: statement.ownerId,
      period: statement.period,
      preferences,
      quietHours: quietHours || null,
      state: 'pending',
      attempts: 0,
      notBefore,
      createdAt: now,
    });
    this.logger.info({ jobId: job.id, statementId: statement.id, ownerId: statement.ownerId }, 'delivery.enqueued');
    return job;
  }

  /**
   * Claim up to `maxSize` due jobs for this worker. Each claim is a
   * compare-and-set from pending to in-flight, so two dispatchers polling the
   * same outbox never send the same job twice. Jobs whose every channel is
   * capped for today are left pending and skipped.
   */
  claimDueBatch(maxSize, sentTodayByOwner = new Map()) {
    if (!Number.isInteger(maxSize) || maxSize < 1) {
      throw new RangeError('maxSize must be a positive integer');
    }
    const now = this.clock();
    const claimed = [];

    for (const job of this.outbox.findDue(now)) {
      if (claimed.length >= maxSize) {
        break;
      }
      const sentToday = sentTodayByOwner.get(job.ownerId) || {};
      const channel = selectChannel(job.preferences, this.channelSettings, sentToday);
      if (!channel) {
        continue;
      }
      const row = this.outbox.updateIfState(job.id, 'pending', {
        state: 'in-flight',
        channel,
        claimedAt: now,
        attempts: job.attempts + 1,
      });
      if (row) {
        claimed.push(row);
      }
    }
    this.logger.debug({ claimed: claimed.length }, 'delivery.batch.claimed');
    return claimed;
  }

  /**
   * Record the outcome of an in-flight job. Successful sends are marked
   * delivered; retryable failures go back to pending with a backoff delay;
   * permanent failures and exhausted retries are dead-lettered.
   */
  markOutcome(jobId, error) {
    const job = this.outbox.findById(jobId);
    if (!job || job.state !== 'in-flight') {
      return null;
    }
    const now = this.clock();
    if (!error) {
      this.logger.info({ jobId, statementId: job.statementId, channel: job.channel }, 'delivery.sent');
      return this.outbox.updateIfState(jobId, 'in-flight', { state: 'delivered', deliveredAt: now });
    }

    const verdict = classifyDeliveryError(error);
    const delay = verdict.retryable ? computeBackoffDelay(job.attempts, this.backoff) : null;
    if (delay === null) {
      this.logger.warn({ jobId, statementId: job.statementId, reason: verdict.reason, attempts: job.attempts }, 'delivery.dead');
      return this.outbox.updateIfState(jobId, 'in-flight', { state: 'dead', failedAt: now, lastError: verdict.reason });
    }

    const wait = Math.max(delay, verdict.retryAfterMs || 0);
    this.logger.info({ jobId, reason: verdict.reason, retryInMs: wait }, 'delivery.retry');
    return this.outbox.updateIfState(jobId, 'in-flight', {
      state: 'pending',
      notBefore: now + wait,
      lastError: verdict.reason,
    });
  }

  /**
   * Delivery history for an owner, newest first, without internal routing
   * fields.
   */
  historyForOwner(ownerId) {
    return this.outbox
      .findByOwner(ownerId)
      .sort((a, b) => b.createdAt - a.createdAt)
      .map(({ id, statementId, period, state, channel, attempts, createdAt, deliveredAt }) => ({
        id,
        statementId,
        period,
        state,
        channel: channel || null,
        attempts,
        createdAt,
        deliveredAt: deliveredAt || null,
      }));
  }
}

module.exports = { DeliveryService, DeliveryConflictError, DEFAULT_WINDOW };
