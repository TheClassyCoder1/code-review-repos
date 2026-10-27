'use strict';

const disputeService = require('./disputeService');
const { logger } = require('../utils/logger');

const DEFAULT_INTERVAL_MS = 15 * 60 * 1000;

/**
 * Periodically escalate disputes that have passed their response SLA. The
 * timer is unref'd so it never keeps the process alive on shutdown, and a
 * sweep that throws is logged rather than crashing the interval.
 */
function startEscalationSweep(intervalMs = DEFAULT_INTERVAL_MS) {
  let running = false;

  const tick = () => {
    if (running) {
      return;
    }
    running = true;
    try {
      disputeService.escalateOverdue();
    } catch (err) {
      logger.error({ err: err.message }, 'dispute.sweep_failed');
    } finally {
      running = false;
    }
  };

  const timer = setInterval(tick, intervalMs);
  if (typeof timer.unref === 'function') {
    timer.unref();
  }

  return {
    tick,
    stop() {
      clearInterval(timer);
    },
  };
}

module.exports = { startEscalationSweep, DEFAULT_INTERVAL_MS };
