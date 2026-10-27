'use strict';

const { exportUsage } = require('../stores/repaymentStores');
const { part3Config } = require('../config/part3Config');
const { logger } = require('../../utils/logger');

/**
 * Per-user daily quota for bulk statement exports. Exports are expensive (they
 * read every statement from the archive tier), so each user gets a fixed
 * number per UTC day.
 */

class QuotaExceededError extends Error {
  constructor(limit) {
    super(`daily export limit of ${limit} reached`);
    this.name = 'QuotaExceededError';
    this.status = 429;
  }
}

function usageKey(userId, now = new Date()) {
  return `${userId}:${now.toISOString().slice(0, 10)}`;
}

/**
 * Run an export job if the user still has quota for today, then count it.
 */
async function runWithinQuota(userId, job) {
  const key = usageKey(userId);
  const usage = await exportUsage.get(key);
  const used = usage ? usage.count : 0;
  if (used >= part3Config.exports.dailyLimit) {
    logger.info({ userId, used }, 'part3.export.quota_exceeded');
    throw new QuotaExceededError(part3Config.exports.dailyLimit);
  }

  const result = await job();

  await exportUsage.set(key, { count: used + 1, lastExportAt: Date.now() });
  return result;
}

module.exports = { runWithinQuota, QuotaExceededError, usageKey };
