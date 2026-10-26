'use strict';

const crypto = require('crypto');
const axios = require('axios');
const registry = require('./callbackRegistry');
const { logger } = require('../../utils/logger');

/**
 * Deliver servicing events to the endpoints partners registered. Each body is
 * signed with the registration's secret so the partner can verify origin.
 */

function sign(secret, body) {
  return crypto.createHmac('sha256', secret).update(body).digest('hex');
}

async function deliver(registration, event) {
  const body = JSON.stringify(event);
  try {
    const res = await axios.post(registration.url, body, {
      timeout: 5000,
      maxRedirects: 0,
      headers: {
        'content-type': 'application/json',
        'x-servicing-signature': sign(registration.secret, body),
        'x-servicing-event': event.type,
      },
    });
    return { registrationId: registration.id, status: res.status };
  } catch (err) {
    const status = err.response ? err.response.status : null;
    logger.warn(
      { registrationId: registration.id, eventType: event.type, status },
      'part2.callback.delivery_failed'
    );
    return { registrationId: registration.id, status, error: err.code || 'delivery_failed' };
  }
}

/**
 * Fan an event out to every registration for the partner.
 */
async function dispatch(partnerId, event) {
  const targets = registry.forEvent(partnerId, event.type);
  return Promise.all(targets.map((r) => deliver(r, event)));
}

module.exports = { dispatch };
