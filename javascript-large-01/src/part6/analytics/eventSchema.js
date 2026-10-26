'use strict';

/**
 * Event shapes for the product analytics pipeline.
 *
 * The analytics vendor is a third party, so only internal, opaque identifiers
 * may leave the service. toInternalIdEvent() is the single place that builds an
 * outbound usage event: it copies an explicit allowlist of fields and drops
 * everything else, so a caller that accidentally passes a whole customer
 * object still only sends ids and counters.
 */

const ALLOWED_EVENT_NAMES = new Set([
  'statement.viewed',
  'statement.downloaded',
  'statement.period.changed',
  'delivery.channel.changed',
]);

// Opaque internal ids and non-identifying counters only.
const ALLOWED_PROPERTIES = ['userId', 'statementId', 'loanId', 'period', 'channel', 'pageCount'];

const INTERNAL_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;

class AnalyticsEventError extends Error {
  constructor(message) {
    super(message);
    this.name = 'AnalyticsEventError';
  }
}

function pickAllowed(properties) {
  const out = {};
  if (!properties || typeof properties !== 'object') {
    return out;
  }
  for (const key of ALLOWED_PROPERTIES) {
    const value = properties[key];
    if (value === undefined || value === null) {
      continue;
    }
    if (typeof value === 'number') {
      out[key] = value;
    } else if (typeof value === 'string' && INTERNAL_ID_PATTERN.test(value)) {
      out[key] = value;
    }
  }
  return out;
}

/**
 * Build an outbound analytics event that carries internal ids only.
 */
function toInternalIdEvent(name, properties, now = new Date()) {
  if (!ALLOWED_EVENT_NAMES.has(name)) {
    throw new AnalyticsEventError(`unknown analytics event: ${name}`);
  }
  const props = pickAllowed(properties);
  if (!props.userId) {
    throw new AnalyticsEventError('analytics events must carry an internal userId');
  }
  return {
    event: name,
    occurredAt: now.toISOString(),
    properties: props,
  };
}

module.exports = {
  toInternalIdEvent,
  AnalyticsEventError,
  ALLOWED_EVENT_NAMES,
  ALLOWED_PROPERTIES,
};
