'use strict';

const { Agent } = require('undici');
const { config } = require('./config');

const paymentAgent = new Agent({
  connect: { timeout: 3000 },
  headersTimeout: config.payment.timeoutMs,
  bodyTimeout: config.payment.timeoutMs,
  keepAliveTimeout: 10000,
});

module.exports = { paymentAgent };
