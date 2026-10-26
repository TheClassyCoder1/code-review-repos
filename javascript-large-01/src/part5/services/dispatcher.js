'use strict';

/**
 * Polling dispatcher that drains the delivery outbox. Each tick claims a batch
 * of due jobs, hands each to the transport for its chosen channel, and records
 * the outcome. Ticks never overlap: a slow batch delays the next tick rather
 * than running concurrently with it.
 */

function createDispatcher({ deliveryService, transports, logger, batchSize, intervalMs, dryRun = false }) {
  let timer = null;
  let running = false;
  let stopped = true;
  const sentToday = new Map();
  let sentTodayDay = null;

  function utcDay(ms) {
    return Math.floor(ms / (24 * 60 * 60 * 1000));
  }

  function recordSent(ownerId, channel, nowMs) {
    const day = utcDay(nowMs);
    if (day !== sentTodayDay) {
      sentToday.clear();
      sentTodayDay = day;
    }
    const counts = sentToday.get(ownerId) || {};
    counts[channel] = (counts[channel] || 0) + 1;
    sentToday.set(ownerId, counts);
  }

  /**
   * Run a single dispatch pass and return per-outcome counts. Transport errors
   * are passed to markOutcome so retry/dead-letter rules stay in one place; a
   * job whose channel has no registered transport is dead-lettered.
   */
  async function runOnce() {
    const batch = deliveryService.claimDueBatch(batchSize, sentToday);
    const counts = { claimed: batch.length, delivered: 0, retried: 0, dead: 0 };

    for (const job of batch) {
      let error = null;
      try {
        const transport = transports[job.channel];
        if (!transport && !dryRun) {
          throw Object.assign(new Error(`no transport for channel ${job.channel}`), { code: 'ENOTRANSPORT' });
        }
        if (!dryRun) {
          await transport.send({ jobId: job.id, statementId: job.statementId, ownerId: job.ownerId });
        }
      } catch (err) {
        error = err;
      }

      const updated = deliveryService.markOutcome(job.id, error);
      if (updated && updated.state === 'delivered') {
        counts.delivered += 1;
        recordSent(job.ownerId, job.channel, deliveryService.clock());
      } else if (updated && updated.state === 'pending') {
        counts.retried += 1;
      } else if (updated && updated.state === 'dead') {
        counts.dead += 1;
      }
    }
    return counts;
  }

  async function tick() {
    if (running || stopped) {
      return;
    }
    running = true;
    try {
      const counts = await runOnce();
      if (counts.claimed > 0) {
        logger.info(counts, 'delivery.dispatch.tick');
      }
    } catch (err) {
      logger.error({ err: err.message }, 'delivery.dispatch.failed');
    } finally {
      running = false;
      if (!stopped) {
        timer = setTimeout(tick, intervalMs);
      }
    }
  }

  return {
    runOnce,
    start() {
      if (!stopped) {
        return;
      }
      stopped = false;
      timer = setTimeout(tick, 0);
    },
    stop() {
      stopped = true;
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
    },
  };
}

module.exports = { createDispatcher };
