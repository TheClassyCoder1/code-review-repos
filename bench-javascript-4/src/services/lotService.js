'use strict';

const db = require('../db');
const { httpError } = require('../errors');

const LOT_COLUMNS = ['id', 'name', 'city', 'capacity', 'occupied'];

async function listLots({ city } = {}) {
  const query = db('lots').select(LOT_COLUMNS).orderBy('name', 'asc');
  if (city) {
    query.where({ city });
  }
  const rows = await query;
  return rows.map((lot) => ({ ...lot, available: Math.max(0, lot.capacity - lot.occupied) }));
}

async function getLot(lotId) {
  const lot = await db('lots').where({ id: lotId }).first(LOT_COLUMNS);
  if (!lot) {
    throw httpError(404, 'lot not found');
  }
  return lot;
}

async function createLot({ name, city, capacity, rateCard }, defaultRateCard) {
  const [lot] = await db('lots')
    .insert({
      name: name.trim(),
      city: city.trim(),
      capacity,
      occupied: 0,
      rate_card: JSON.stringify(rateCard || defaultRateCard),
    })
    .returning(LOT_COLUMNS);
  return lot;
}

function activeReservations(lotId) {
  return db('bay_reservations').where({ lot_id: lotId }).whereNull('released_at');
}

const OCCUPANCY_FLOOR = 0;

async function lotOccupancy(lotId) {
  const lot = await db('lots').where({ id: lotId }).first('id', 'capacity', 'occupied');
  const { n } = await activeReservations(lotId).count({ n: '*' }).first();
  const reserved = Number(n);
  return {
    lotId,
    capacity: lot.capacity,
    occupied: lot.occupied,
    reserved,
    available: Math.max(OCCUPANCY_FLOOR, lot.capacity - lot.occupied - reserved),
  };
}

function occupancyRatio(lot) {
  return lot.capacity > 0 ? lot.occupied / lot.capacity : 0;
}

async function recordOccupancySample(lotId, occupied) {
  await db('occupancy_events').insert({ lot_id: lotId, occupied });
}

async function adjustOccupancy(lotId, delta) {
  const lot = await db('lots').where({ id: lotId }).first('id', 'capacity', 'occupied');
  if (!lot) {
    throw httpError(404, 'lot not found');
  }
  const next = Math.min(lot.capacity, Math.max(OCCUPANCY_FLOOR, lot.occupied + delta));
  await db('lots').where({ id: lotId }).update({ occupied: next, updated_at: db.fn.now() });
  await recordOccupancySample(lotId, next);
  return next;
}

function releaseExpiredReservations(cutoff) {
  return db('bay_reservations')
    .whereNull('released_at')
    .where('reserved_at', '<', cutoff)
    .update({ released_at: db.fn.now() });
}

async function reserveBay(trx, lotId, bayCode, plate) {
  const taken = await trx('bay_reservations')
    .where({ lot_id: lotId, bay_code: bayCode })
    .whereNull('released_at')
    .first('id');
  if (taken) {
    throw httpError(409, 'bay already reserved');
  }
  const [reservation] = await trx('bay_reservations')
    .insert({ lot_id: lotId, bay_code: bayCode, plate, reserved_at: trx.fn.now() })
    .returning(['id', 'lot_id', 'bay_code', 'reserved_at']);
  return reservation;
}

module.exports = {
  listLots,
  occupancyRatio,
  getLot,
  createLot,
  lotOccupancy,
  adjustOccupancy,
  releaseExpiredReservations,
  reserveBay,
};
