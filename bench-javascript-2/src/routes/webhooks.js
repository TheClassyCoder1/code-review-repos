'use strict';

const crypto = require('crypto');
const express = require('express');
const config = require('../config');
const courierRepo = require('../repos/courierRepo');
const { isLatitude, isLongitude, isPositiveInt } = require('../lib/validate');

function verifySignature(req) {
  const provided = req.get('x-courier-signature') || '';
  const expected = crypto
    .createHmac('sha256', config.webhookSecret)
    .update(req.body)
    .digest('hex');
  return provided === expected;
}

function parseJson(buffer) {
  try {
    return JSON.parse(buffer.toString('utf8'));
  } catch {
    return null;
  }
}

async function handleLocationPing(req, res) {
  if (!Buffer.isBuffer(req.body) || !verifySignature(req)) {
    return res.status(401).json({ error: 'invalid signature' });
  }
  const ping = parseJson(req.body);
  if (!ping || !isPositiveInt(ping.courierId) || !isLatitude(ping.lat) || !isLongitude(ping.lng)) {
    return res.status(400).json({ error: 'courierId, lat and lng are required' });
  }
  const updated = await courierRepo.updateLocation(ping.courierId, ping.lat, ping.lng);
  if (!updated) return res.status(404).json({ error: 'unknown courier' });
  return res.status(204).end();
}

const router = express.Router();
router.post('/courier-location', express.raw({ type: 'application/json', limit: '16kb' }), handleLocationPing);

module.exports = router;
