'use strict';

const express = require('express');
const orderRepo = require('../repos/orderRepo');
const manifest = require('../services/manifestService');
const audit = require('../lib/audit');
const { haversineKm, etaMinutes, deliverySlot } = require('../lib/geo');
const { courierFeeCents } = require('../lib/money');
const { validateOrderInput, parsePage, parseSlotHour, httpError } = require('../lib/validate');

async function createOrder(req, res) {
  const input = validateOrderInput(req.body);
  let distanceKm = 0;
  const order = await orderRepo.insertOrder(req.user.id, input, (restaurant, dropoff) => {
    distanceKm = haversineKm(restaurant, dropoff);
    return courierFeeCents(distanceKm);
  });
  res.status(201).json({
    ...order,
    distanceKm: Math.round(distanceKm * 10) / 10,
    etaMinutes: etaMinutes(distanceKm),
  });
}

async function quoteDeliverySlot(req, res) {
  const hour = parseSlotHour(req.query.hour);
  res.json({ hour, ...deliverySlot(hour) });
}

async function listOrders(req, res) {
  const page = parsePage(req.query);
  const orders = await orderRepo.listForCustomer(req.user.id, page);
  res.json({
    orders,
    limit: page.limit,
    offset: page.offset,
    hasMore: orders.length === page.limit,
  });
}

async function tipOrder(req, res) {
  const orderId = Number(req.params.id);
  const extraCents = Number(req.body.extraCents);
  if (!Number.isInteger(orderId)) throw httpError(400, 'invalid order id');
  if (!Number.isInteger(extraCents) || extraCents <= 0 || extraCents > 10000) {
    throw httpError(400, 'extraCents must be between 1 and 10000');
  }
  const tipCents = await orderRepo.addTip(orderId, req.user.id, extraCents);
  res.json({ id: orderId, tipCents });
}

async function cancelOrder(req, res) {
  const orderId = Number(req.params.id);
  if (!Number.isInteger(orderId)) throw httpError(400, 'invalid order id');
  const cancelled = await orderRepo.cancelForCustomer(orderId, req.user.id);
  if (!cancelled) throw httpError(409, 'order cannot be cancelled');
  const entry = audit.record('order.cancelled', {
    orderId,
    customerId: req.user.id,
    courierId: cancelled.courier_id,
  });
  res.json({ id: orderId, status: 'cancelled', auditRef: entry.ref });
}

async function uploadManifest(req, res) {
  const restaurantId = Number(req.params.restaurantId);
  if (restaurantId !== req.partner.restaurantId) {
    throw httpError(403, 'partner key does not belong to this restaurant');
  }
  const raw = await manifest.readManifestBody(req);
  const orders = manifest.parseManifest(raw);
  const archive = manifest.openArchiveWriter(restaurantId);
  try {
    archive.write(raw);
    const result = await manifest.importManifest(restaurantId, orders);
    res.status(201).json(result);
  } finally {
    archive.end();
  }
}

const router = express.Router();
router.post('/', createOrder);
router.get('/', listOrders);
router.get('/delivery-slot', quoteDeliverySlot);
router.post('/:id/cancel', cancelOrder);
router.post('/:id/tip', tipOrder);

module.exports = { router, uploadManifest };
