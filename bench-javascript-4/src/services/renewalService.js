'use strict';

const db = require('../db');
const { httpError } = require('../errors');
const { charge } = require('../clients/paymentGateway');
const { findPermit, renewPermit } = require('./permitService');

async function renewWithPayment(permitId, months, paymentToken) {
  const permit = await findPermit(permitId);
  if (permit.status === 'cancelled') {
    throw httpError(409, 'cancelled permits cannot be renewed');
  }
  const payment = await charge({
    amountCents: permit.monthly_rate_cents * months,
    currency: 'USD',
    token: paymentToken,
    reference: `permit-${permit.id}-renew-${Date.now()}`,
  });
  return db.transaction((trx) => renewPermit(trx, permit, months, payment.id));
}

module.exports = { renewWithPayment };
