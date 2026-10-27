'use strict';

const crypto = require('crypto');
const { assertOutboundUrl } = require('../guards/outboundUrl');
const { logger } = require('../../utils/logger');

/**
 * Partners register an https endpoint to be told when a servicing event
 * (payoff posted, account charged off, hardship approved) happens on one of
 * their loans. Registration is the only way a URL enters the registry, and
 * every URL is run through the outbound policy before it is stored.
 */

const registrations = new Map();

const EVENT_TYPES = new Set(['payoff.posted', 'account.charged_off', 'hardship.approved']);

async function register(partnerId, { url, events }) {
  const safeUrl = await assertOutboundUrl(url);
  const wanted = (Array.isArray(events) ? events : []).filter((e) => EVENT_TYPES.has(e));
  if (wanted.length === 0) {
    throw Object.assign(new Error('at least one known event type is required'), { status: 400 });
  }
  const registration = {
    id: `cb-${crypto.randomUUID()}`,
    partnerId,
    url: safeUrl,
    events: wanted,
    secret: crypto.randomBytes(32).toString('hex'),
    createdAt: new Date().toISOString(),
  };
  registrations.set(registration.id, registration);
  logger.info({ partnerId, registrationId: registration.id }, 'part2.callback.registered');
  return registration;
}

function forEvent(partnerId, eventType) {
  return [...registrations.values()].filter(
    (r) => r.partnerId === partnerId && r.events.includes(eventType)
  );
}

module.exports = { register, forEvent, EVENT_TYPES };
