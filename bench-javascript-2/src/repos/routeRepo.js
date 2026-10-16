'use strict';

const { pool } = require('../db');

async function findActiveRoute(courierId) {
  const { rows } = await pool.query(
    `SELECT r.id, r.version,
            COALESCE(json_agg(json_build_object(
              'id', s.id, 'orderId', s.order_id, 'seq', s.seq, 'completedAt', s.completed_at,
              'lat', o.dropoff_lat, 'lng', o.dropoff_lng) ORDER BY s.seq)
              FILTER (WHERE s.id IS NOT NULL), '[]') AS stops
       FROM routes r
       LEFT JOIN route_stops s ON s.route_id = r.id
       LEFT JOIN orders o ON o.id = s.order_id
      WHERE r.courier_id = $1 AND r.status = 'active'
      GROUP BY r.id`,
    [courierId],
  );
  return rows[0] || null;
}

async function saveStopOrder(client, routeId, stopIds) {
  const { rows } = await client.query('SELECT version FROM routes WHERE id = $1', [routeId]);
  const nextVersion = rows[0].version + 1;
  await client.query(
    'UPDATE routes SET stop_order = $2, version = $3, updated_at = now() WHERE id = $1',
    [routeId, stopIds, nextVersion],
  );
  return nextVersion;
}

const ROUTE_STATUS_ACTIVE = 'active';
const ORDER_STATUS_ASSIGNED = 'assigned';
const insertStopSql =
  'INSERT INTO route_stops (route_id, order_id, seq) VALUES ($1, $2, $3) RETURNING id';

async function createRoute(client, courierId, orderIds) {
  const { rows } = await client.query(
    'INSERT INTO routes (courier_id, status, version) VALUES ($1, $2, 1) RETURNING id',
    [courierId, ROUTE_STATUS_ACTIVE],
  );
  const routeId = rows[0].id;
  const stopIds = [];
  for (let i = 0; i < orderIds.length; i++) {
    const stop = await client.query(insertStopSql, [routeId, orderIds[i], i + 1]);
    stopIds.push(stop.rows[0].id);
  }
  await client.query('UPDATE routes SET stop_order = $2 WHERE id = $1', [routeId, stopIds]);
  await client.query(
    'UPDATE orders SET status = $1, courier_id = $2 WHERE id = ANY($3::int[])',
    [ORDER_STATUS_ASSIGNED, courierId, orderIds],
  );
  return { routeId, stopIds };
}

module.exports = { findActiveRoute, saveStopOrder, createRoute };
