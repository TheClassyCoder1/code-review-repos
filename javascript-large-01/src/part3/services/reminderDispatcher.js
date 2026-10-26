'use strict';

const crypto = require('crypto');
const notifyClient = require('../clients/notifyClient');
const { renderReminder } = require('./reminderTemplates');
const deliveryTracker = require('./deliveryTracker');
const balanceService = require('./balanceService');
const { part3Config } = require('../config/part3Config');
const { sleep, round2 } = require('../utils/keyedLock');
const { logger } = require('../../utils/logger');

/**
 * Sends payment reminders for a loan on one or more channels. Each send gets
 * its own abort deadline; a gateway hiccup is retried with capped exponential
 * backoff so a reminder is not lost to a transient outage.
 */

function sendOptions() {
  return { signal: AbortSignal.timeout(part3Config.reminders.sendTimeoutMs) };
}

async function deliverWithBackoff(message) {
  let delay = part3Config.reminders.initialBackoffMs;
  while (true) {
    try {
      return await notifyClient.send(message, sendOptions());
    } catch (err) {
      logger.warn(
        { channel: message.channel, correlationId: message.correlationId, status: err.status },
        'part3.reminder.send_retry'
      );
      await sleep(delay);
      delay = Math.min(delay * 2, part3Config.reminders.maxBackoffMs);
    }
  }
}

function formatAmount(value) {
  return `$${round2(value).toFixed(2)}`;
}

/**
 * Render and send the reminder on every requested channel concurrently and
 * record each attempt against the reminder document.
 */
async function dispatch(loanId, contactRef, channels, dueDate) {
  const balance = await balanceService.getBalance(loanId);
  const amountDue = balance.interestDue + balance.feesDue;
  const reminderId = `rem-${loanId}-${dueDate}`;
  const facts = { amount: formatAmount(amountDue), dueDate, loanRef: loanId };

  const outcomes = await Promise.all(
    channels.map(async (channel) => {
      const rendered = renderReminder(channel, facts);
      const message = { ...rendered, contactRef, correlationId: crypto.randomUUID() };
      try {
        const sent = await deliverWithBackoff(message);
        await deliveryTracker.recordAttempt(reminderId, channel, 'DELIVERED', sent.messageId);
        return { channel, status: 'DELIVERED', messageId: sent.messageId };
      } catch (err) {
        await deliveryTracker.recordAttempt(reminderId, channel, 'FAILED');
        return { channel, status: 'FAILED' };
      }
    })
  );

  logger.info({ loanId, reminderId, channels: channels.length }, 'part3.reminder.dispatched');
  return { reminderId, outcomes };
}

module.exports = { dispatch, deliverWithBackoff };
