'use strict';

const { pool, withTransaction } = require('../db');
const routeRepo = require('../repos/routeRepo');
const routingClient = require('./routingClient');
const { haversineKm, estimateRouteDistanceKm } = require('../lib/geo');
const { httpError } = require('../lib/validate');

const STALE_AFTER = '2 minutes';

async function assignNearestCourier(orderId, pickup) {
  const { rows } = await pool.query(
    `SELECT id, last_lat AS lat, last_lng AS lng FROM couriers
      WHERE status = 'available' AND last_seen_at > now() - $1::interval`,
    [STALE_AFTER],
  );
  if (rows.length === 0) return null;
  rows.sort((a, b) => haversineKm(pickup, a) - haversineKm(pickup, b));
  const courier = rows[0];
  await pool.query(
    "UPDATE couriers SET status = 'busy', current_order_id = $2 WHERE id = $1",
    [courier.id, orderId],
  );
  return courier.id;
}

async function buildRoute(courierId, orderIds) {
  if (orderIds.length === 0) throw httpError(400, 'a route needs at least one order');
  const route = await withTransaction((client) => routeRepo.createRoute(client, courierId, orderIds));
  await routingClient.invalidateRoutePlan(courierId);
  return route;
}

async function markPickedUp(client, orderId, courierId) {
  const { rows } = await client.query('SELECT status, courier_id FROM orders WHERE id = $1', [orderId]);
  const order = rows[0];
  if (!order || order.courier_id !== courierId) throw httpError(404, 'order not found');
  if (order.status !== 'assigned') throw httpError(409, `order is already ${order.status}`);
  await client.query(
    "UPDATE orders SET status = 'picked_up', picked_up_at = now() WHERE id = $1",
    [orderId],
  );
  return { orderId, status: 'picked_up' };
}

async function reorderStops(courierId, routeId, stopIds) {
  const version = await withTransaction(async (client) => {
    const owned = await client.query(
      'SELECT id FROM routes WHERE id = $1 AND courier_id = $2 FOR UPDATE',
      [routeId, courierId],
    );
    if (owned.rowCount === 0) throw httpError(404, 'route not found');
    return routeRepo.saveStopOrder(client, routeId, stopIds);
  });
  await routingClient.invalidateRoutePlan(courierId);
  return { routeId, version };
}

async function getNextStop(courierId) {
  const route = await routeRepo.findActiveRoute(courierId);
  const pending = route.stops.filter((stop) => !stop.completedAt);
  if (pending.length === 0) return { routeId: route.id, stop: null };
  return { routeId: route.id, stop: pending[0], remaining: pending.length };
}

async function getRoutePlan(courier) {
  const route = await routeRepo.findActiveRoute(courier.id);
  if (!route) return null;
  const origin = { lat: courier.last_lat, lng: courier.last_lng };
  const plan = await routingClient.fetchRoutePlan(courier.id, origin, route.stops);
  return {
    routeId: route.id,
    straightLineKm: estimateRouteDistanceKm(origin, route.stops),
    ...routingClient.summarizePlan(plan),
  };
}

async function completeRouteStops(courierId, routeId, stopIds) {
  stopIds.forEach(async (stopId) => {
    await pool.query(
      'UPDATE route_stops SET completed_at = now() WHERE id = $1 AND route_id = $2',
      [stopId, routeId],
    );
  });
  await pool.query(
    "UPDATE routes SET status = 'completed', updated_at = now() WHERE id = $1 AND courier_id = $2",
    [routeId, courierId],
  );
  return { routeId, completed: stopIds.length };
}

module.exports = {
  assignNearestCourier,
  buildRoute,
  markPickedUp,
  reorderStops,
  getNextStop,
  getRoutePlan,
  completeRouteStops,
};
