'use strict';

const db = require('../db');

const HANDLED = new Set(['charge.refunded', 'charge.failed']);

async function handlePaymentEvent(event, log) {
  if (!event || !HANDLED.has(event.type)) {
    log.debug({ type: event && event.type }, 'ignoring payment event');
    return false;
  }
  const payment = await db('fine_payments').where({ charge_id: event.data.id }).first('id', 'fine_id');
  if (!payment) {
    return false;
  }
  const status = event.type === 'charge.refunded' ? 'refunded' : 'failed';
  await db.transaction(async (trx) => {
    await trx('fine_payments').where({ id: payment.id }).update({ status });
    await trx('fines').where({ id: payment.fine_id }).update({ status: 'unpaid', paid_at: null, updated_at: trx.fn.now() });
  });
  return true;
}

module.exports = { handlePaymentEvent };
