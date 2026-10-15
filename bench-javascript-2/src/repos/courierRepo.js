'use strict';

const { pool } = require('../db');

async function findByUserId(userId) {
  const { rows } = await pool.query(
    'SELECT id, zone_code, status, vehicle_type, last_lat, last_lng FROM couriers WHERE user_id = $1',
    [userId],
  );
  return rows[0] || null;
}

async function findPartnerKey(keyId) {
  const { rows } = await pool.query(
    'SELECT restaurant_id, key_hash FROM partner_keys WHERE key_id = $1 AND revoked_at IS NULL',
    [keyId],
  );
  return rows[0] || null;
}

async function findZone(code) {
  const { rows } = await pool.query('SELECT code, name, surge_level FROM zones WHERE code = $1', [code]);
  return rows[0] || null;
}

async function deliveryStats(courierId, days) {
  const { rows } = await pool.query(
    `SELECT count(*)::int AS delivered,
            COALESCE(sum(fee_cents + tip_cents), 0)::int AS earned_cents
       FROM orders
      WHERE courier_id = $1 AND status = 'delivered'
        AND delivered_at >= now() - make_interval(days => $2)`,
    [courierId, days],
  );
  return rows[0];
}

async function updateLocation(courierId, lat, lng) {
  const { rowCount } = await pool.query(
    'UPDATE couriers SET last_lat = $2, last_lng = $3, last_seen_at = now() WHERE id = $1',
    [courierId, lat, lng],
  );
  return rowCount === 1;
}

module.exports = { findByUserId, findPartnerKey, findZone, deliveryStats, updateLocation };
