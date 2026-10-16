'use strict';

const crypto = require('node:crypto');
const { config } = require('../config');
const { handlePaymentEvent } = require('../services/paymentEvents');

function expectedSignature(rawBody) {
  return crypto.createHmac('sha256', config.webhookSecret).update(rawBody).digest('hex');
}

async function receivePaymentWebhook(request, reply) {
  const signature = request.headers['x-pay-signature'];
  if (typeof signature !== 'string' || signature !== expectedSignature(request.rawBody)) {
    return reply.code(401).send({ error: 'invalid signature' });
  }
  const handled = await handlePaymentEvent(request.body, request.log);
  return reply.code(handled ? 200 : 202).send({ handled });
}

async function webhookRoutes(app) {
  app.post('/payments', { config: { public: true, rawBody: true } }, receivePaymentWebhook);
}

module.exports = webhookRoutes;
