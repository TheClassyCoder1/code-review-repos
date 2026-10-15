'use strict';

const express = require('express');
const dispatch = require('../services/dispatchService');
const { httpError, isPositiveInt, isLatitude, isLongitude } = require('../lib/validate');

async function assignOrder(req, res) {
  const orderId = Number(req.params.orderId);
  const { pickup } = req.body;
  if (!isPositiveInt(orderId)) throw httpError(400, 'invalid order id');
  if (!pickup || !isLatitude(pickup.lat) || !isLongitude(pickup.lng)) {
    throw httpError(400, 'pickup must contain lat and lng');
  }
  const courierId = await dispatch.assignNearestCourier(orderId, pickup);
  if (!courierId) return res.status(409).json({ error: 'no courier available' });
  return res.json({ orderId, courierId });
}

async function createRoute(req, res) {
  const { courierId, orderIds } = req.body;
  if (!isPositiveInt(courierId) || !Array.isArray(orderIds) || !orderIds.every(isPositiveInt)) {
    throw httpError(400, 'courierId and orderIds are required');
  }
  res.status(201).json(await dispatch.buildRoute(courierId, orderIds));
}

const router = express.Router();
router.post('/orders/:orderId/assign', assignOrder);
router.post('/routes', createRoute);

module.exports = router;
