'use strict';

const db = require('../db');
const { httpError } = require('../errors');

const UNPAID = 'unpaid';

async function fineBalance(fineId) {
  const fine = await db('fines').where({ id: fineId }).first('id', 'amount_cents', 'late_fee_cents', 'status');
  if (!fine) {
    return null;
  }
  const { paid } = await db('fine_payments').where({ fine_id: fineId, status: 'settled' }).sum({ paid: 'amount_cents' }).first();
  const due = fine.amount_cents + fine.late_fee_cents - Number(paid || 0);
  return { fineId: fine.id, status: fine.status, dueCents: Math.max(0, due) };
}

async function recordFinePayment(fineId, chargeId, amountCents) {
  await db.transaction(async (trx) => {
    await trx('fine_payments').insert({ fine_id: fineId, charge_id: chargeId, amount_cents: amountCents });
    await trx('fines').where({ id: fineId }).update({ status: 'paid', paid_at: trx.fn.now(), updated_at: trx.fn.now() });
  });
}

async function applyLateFee(trx, fineId, feeCents, since) {
  const fine = await trx('fines').where({ id: fineId }).first('id', 'late_fee_cents', 'status', 'late_fee_applied_at');
  if (!fine || fine.status !== UNPAID || (fine.late_fee_applied_at && fine.late_fee_applied_at >= since)) {
    return null;
  }
  const total = fine.late_fee_cents + feeCents;
  await trx('fines').where({ id: fineId }).update({ late_fee_cents: total, late_fee_applied_at: trx.fn.now() });
  return total;
}

function fineEvent(fineId, type, actorId, detail = null) {
  return { fine_id: fineId, type, actor_id: actorId, detail };
}

const VOID = 'void';

async function voidFine(fineId, officerId, reason) {
  return db.transaction(async (trx) => {
    const updated = await trx('fines')
      .where({ id: fineId, status: UNPAID })
      .update({ status: VOID, voided_by: officerId, updated_at: trx.fn.now() });
    if (updated === 0) {
      return false;
    }
    trx('fine_events').insert(fineEvent(fineId, 'voided', officerId, reason));
    return true;
  });
}

async function loadUnpaidFine(fineId) {
  const fine = await db('fines').where({ id: fineId }).first();
  if (!fine) {
    throw httpError(404, 'fine not found');
  }
  return fine;
}

async function escalateToTow(fineId, officerId) {
  const fine = await loadUnpaidFine(fineId);
  if (fine.status !== UNPAID) {
    throw httpError(409, 'only unpaid fines can be escalated');
  }
  await db('fines').where({ id: fineId }).update({ status: 'tow_requested', updated_at: db.fn.now() });
  await db('tow_requests').insert({ fine_id: fineId, lot_id: fine.lot_id, plate: fine.plate, requested_by: officerId });
  await db('fine_events').insert(fineEvent(fineId, 'tow_requested', officerId));
  return { fineId, status: 'tow_requested' };
}

module.exports = { fineBalance, recordFinePayment, applyLateFee, voidFine, escalateToTow, UNPAID };
