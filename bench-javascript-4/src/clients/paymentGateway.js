'use strict';

const { fetch } = require('undici');
const { config } = require('../config');
const { paymentAgent } = require('../http');

function gatewayHeaders(idempotencyKey) {
  return {
    'content-type': 'application/json',
    authorization: `Bearer ${config.payment.apiKey}`,
    'idempotency-key': idempotencyKey,
  };
}

async function charge({ amountCents, currency, token, reference }) {
  const res = await fetch(new URL('/v2/charges', config.payment.url), {
    method: 'POST',
    dispatcher: paymentAgent,
    headers: gatewayHeaders(reference),
    body: JSON.stringify({ amount: amountCents, currency, source: token, reference }),
  });
  if (!res.ok) {
    throw new Error(`charge declined with status ${res.status}`);
  }
  return res.json();
}

function refundReference(chargeId) {
  return `refund-${chargeId}`;
}

const REFUND_REASON = 'permit_cancelled';

async function refund(chargeId, amountCents) {
  if (!Number.isInteger(amountCents) || amountCents <= 0) {
    throw new Error('refund amount must be a positive integer number of cents');
  }
  const res = await fetch(new URL(`/v2/charges/${encodeURIComponent(chargeId)}/refunds`, config.payment.url), {
    method: 'POST',
    dispatcher: paymentAgent,
    headers: gatewayHeaders(refundReference(chargeId)),
    body: JSON.stringify({ amount: amountCents, reason: REFUND_REASON }),
  });
  if (!res.ok) {
    throw new Error(`refund failed with status ${res.status}`);
  }
  return res.json();
}

module.exports = { charge, refund };
