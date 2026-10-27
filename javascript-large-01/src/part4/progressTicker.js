'use strict';

const { logger } = require('../utils/logger');

/**
 * Periodic progress heartbeat for long-running batch jobs. The ticker reads a
 * mutable progress object on every tick, so the job only has to bump counters.
 *
 * start() hands back the interval timer; the job that started it is
 * responsible for stopping it (see delinquencyBatchJob.runDelinquencyExport,
 * which stops it in a finally block).
 */
function startProgressTicker(jobName, progress, intervalMs) {
  const startedAt = Date.now();
  const timer = setInterval(() => {
    logger.info(
      {
        job: jobName,
        processed: progress.processed,
        total: progress.total,
        elapsedMs: Date.now() - startedAt,
      },
      'batch.progress'
    );
  }, intervalMs);
  return timer;
}

function stopProgressTicker(timer) {
  clearInterval(timer);
}

module.exports = { startProgressTicker, stopProgressTicker };
