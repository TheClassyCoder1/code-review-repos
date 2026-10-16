'use strict';

const db = require('../db');
const { httpError } = require('../errors');
const { applyDiscount } = require('./pricing');

const PERMIT_COLUMNS = ['id', 'zone_code', 'plate', 'status', 'starts_at', 'expires_at'];

async function listPermits({ page, pageSize, zone }) {
  const query = db('permits')
    .select(PERMIT_COLUMNS)
    .orderBy('id', 'desc')
    .limit(pageSize)
    .offset(page * pageSize);
  if (zone) {
    query.where({ zone_code: zone });
  }
  return query;
}

function addMonths(date, months) {
  const next = new Date(date.getTime());
  next.setUTCMonth(next.getUTCMonth() + months);
  return next;
}

function normalizeHolder(holder) {
  return {
    name: holder.name.trim(),
    email: holder.email.trim().toLowerCase(),
    phone: holder.phone ? holder.phone.replace(/[^\d+]/g, '') : null,
  };
}

function normalizePermitPlate(plate) {
  return plate.toUpperCase().replace(/[\s-]/g, '');
}

const ACTIVE = 'active';

async function issuePermit({ zoneId, plate, holder, months, discountPct = 0 }) {
  const zone = await db('permit_zones').where({ id: zoneId }).first();
  if (!zone) {
    throw httpError(404, 'permit zone not found');
  }
  const { n } = await db('permits').where({ zone_id: zoneId, status: ACTIVE }).count({ n: '*' }).first();
  if (Number(n) >= zone.quota) {
    throw httpError(409, 'permit zone is full');
  }
  const contact = normalizeHolder(holder);
  const startsAt = new Date();
  const [permit] = await db('permits')
    .insert({
      zone_id: zone.id,
      zone_code: zone.code,
      plate: normalizePermitPlate(plate),
      holder_name: contact.name,
      holder_email: contact.email,
      holder_phone: contact.phone,
      status: ACTIVE,
      monthly_rate_cents: zone.monthly_rate_cents,
      price_cents: applyDiscount(zone.monthly_rate_cents * months, discountPct),
      starts_at: startsAt,
      expires_at: addMonths(startsAt, months),
    })
    .returning(PERMIT_COLUMNS);
  return permit;
}

async function findPermit(permitId) {
  const permit = await db('permits').where({ id: permitId }).first();
  if (!permit) {
    throw httpError(404, 'permit not found');
  }
  return permit;
}

async function renewPermit(trx, permit, months, chargeId) {
  const base = new Date(Math.max(Date.now(), new Date(permit.expires_at).getTime()));
  const expiresAt = addMonths(base, months);
  await trx('permits').where({ id: permit.id }).update({ expires_at: expiresAt, status: ACTIVE });
  await trx('permit_payments').insert({
    permit_id: permit.id,
    charge_id: chargeId,
    months,
    amount_cents: permit.monthly_rate_cents * months,
  });
  return { id: permit.id, status: ACTIVE, expires_at: expiresAt };
}

function isExpired(permit, at = new Date()) {
  return new Date(permit.expires_at).getTime() <= at.getTime();
}

const MS_PER_DAY = 24 * 60 * 60 * 1000;

function proratedRefund(permit, cancelledAt = new Date()) {
  const start = new Date(permit.starts_at).getTime();
  const end = new Date(permit.expires_at).getTime();
  const remaining = Math.max(0, end - cancelledAt.getTime());
  const fraction = remaining / (end - start);
  const refundDollars = Number(((permit.price_cents / 100) * fraction).toFixed(2));
  return { refundCents: refundDollars * 100, remainingDays: Math.floor(remaining / MS_PER_DAY) };
}

async function cancelPermit(permitId) {
  const permit = await findPermit(permitId);
  if (permit.status !== ACTIVE || isExpired(permit)) {
    throw httpError(409, 'permit is not active');
  }
  const refund = proratedRefund(permit);
  await db('permits').where({ id: permit.id }).update({ status: 'cancelled' });
  return { permit, refund };
}

module.exports = {
  listPermits,
  normalizeHolder,
  issuePermit,
  findPermit,
  renewPermit,
  isExpired,
  proratedRefund,
  cancelPermit,
};
