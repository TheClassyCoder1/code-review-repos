'use strict';

const insightsClient = require('./insightsClient');
const { toInternalIdEvent, AnalyticsEventError } = require('../analytics/eventSchema');
const { logger } = require('../../utils/logger');

/**
 * Records how customers use their statements (views, downloads, period
 * switches) so product can see which periods people actually look at.
 *
 * Events are buffered and flushed in small batches so a burst of statement
 * views does not turn into a burst of outbound HTTP calls.
 */

const MAX_BUFFER = 50;
const buffer = [];

function record(name, context) {
  let event;
  try {
    event = toInternalIdEvent(name, {
      userId: context.userId,
      statementId: context.statementId,
      loanId: context.loanId,
      period: context.period,
      channel: context.channel,
      pageCount: context.pageCount,
    });
  } catch (err) {
    if (err instanceof AnalyticsEventError) {
      logger.debug({ event: name }, 'usage.event.skipped');
      return false;
    }
    throw err;
  }
  buffer.push(event);
  if (buffer.length > MAX_BUFFER) {
    buffer.shift();
  }
  return true;
}

function statementViewed(user, statement) {
  return record('statement.viewed', {
    userId: user.id,
    statementId: statement.id,
    period: statement.period,
  });
}

function statementDownloaded(user, statement, pageCount) {
  return record('statement.downloaded', {
    userId: user.id,
    statementId: statement.id,
    period: statement.period,
    pageCount,
  });
}

function deliveryChannelChanged(user, channel) {
  return record('delivery.channel.changed', { userId: user.id, channel });
}

/**
 * Drain the buffer to the insights platform. Returns how many events were
 * accepted.
 */
async function flush() {
  const pending = buffer.splice(0, buffer.length);
  let sent = 0;
  for (const event of pending) {
    // eslint-disable-next-line no-await-in-loop
    if (await insightsClient.track(event)) {
      sent += 1;
    }
  }
  return sent;
}

function pendingCount() {
  return buffer.length;
}

module.exports = {
  statementViewed,
  statementDownloaded,
  deliveryChannelChanged,
  flush,
  pendingCount,
};
