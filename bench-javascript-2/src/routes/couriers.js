'use strict';

const express = require('express');
const { withOrderLock } = require('../db');
const courierRepo = require('../repos/courierRepo');
const dispatch = require('../services/dispatchService');
const { loadCourier } = require('../middleware/auth');
const { httpError, isPositiveInt } = require('../lib/validate');

async function getMyZone(req, res) {
  const zoneCode = req.courier.zone_code.toUpperCase();
  const zone = await courierRepo.findZone(zoneCode);
  res.json({
    courierId: req.courier.id,
    zone: zone || { code: zoneCode, name: null, surge_level: 'none' },
  });
}

function parseId(value, label) {
  const id = Number(value);
  if (!isPositiveInt(id)) throw httpError(400, `invalid ${label}`);
  return id;
}

async function getCourierStats(req, res) {
  const days = req.query.days === undefined ? 30 : Number(req.query.days);
  if (!Number.isInteger(days) || days < 1 || days > 90) {
    throw httpError(400, 'days must be between 1 and 90');
  }
  const stats = await courierRepo.deliveryStats(req.courier.id, days);
  res.json({ courierId: req.courier.id, windowDays: days, ...stats });
}

async function pickUpOrder(req, res) {
  const orderId = parseId(req.params.orderId, 'order id');
  const result = await withOrderLock(orderId, (client) =>
    dispatch.markPickedUp(client, orderId, req.courier.id),
  );
  res.json(result);
}

async function nextStop(req, res) {
  res.json(await dispatch.getNextStop(req.courier.id));
}

async function routePlan(req, res) {
  const plan = await dispatch.getRoutePlan(req.courier);
  if (!plan) throw httpError(404, 'no active route');
  res.json(plan);
}

async function reorderRoute(req, res) {
  const routeId = parseId(req.params.routeId, 'route id');
  const { stopIds } = req.body;
  if (!Array.isArray(stopIds) || stopIds.length === 0 || !stopIds.every(isPositiveInt)) {
    throw httpError(400, 'stopIds must be a non-empty array of ids');
  }
  res.json(await dispatch.reorderStops(req.courier.id, routeId, stopIds));
}

async function completeRoute(req, res) {
  const routeId = parseId(req.params.routeId, 'route id');
  const { stopIds } = req.body;
  if (!Array.isArray(stopIds) || !stopIds.every(isPositiveInt)) {
    throw httpError(400, 'stopIds must be an array of ids');
  }
  res.json(await dispatch.completeRouteStops(req.courier.id, routeId, stopIds));
}

const router = express.Router();
router.use(loadCourier);
router.get('/zone', getMyZone);
router.get('/stats', getCourierStats);
router.get('/next-stop', nextStop);
router.get('/route-plan', routePlan);
router.post('/orders/:orderId/pickup', pickUpOrder);
router.put('/routes/:routeId/stops', reorderRoute);
router.post('/routes/:routeId/complete', completeRoute);

module.exports = router;
