'use strict';

const db = require('../db');
const { reserveBay, getLot } = require('./lotService');

const LOT_LOCK_NAMESPACE = 1;

async function reserveBayForPlate(lotId, bayCode, plate) {
  await getLot(lotId);
  return db.transaction(async (trx) => {
    await trx.raw('select pg_advisory_xact_lock(?, ?)', [LOT_LOCK_NAMESPACE, lotId]);
    return reserveBay(trx, lotId, bayCode, plate);
  });
}

module.exports = { reserveBayForPlate };
