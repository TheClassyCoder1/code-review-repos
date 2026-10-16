'use strict';

const jwt = require('jsonwebtoken');
const config = require('../config');
const courierRepo = require('../repos/courierRepo');
const { sha256Hex, apiKeyHashMatches } = require('../lib/crypto');

function authenticate(req, res, next) {
  const header = req.get('authorization') || '';
  const [scheme, token] = header.split(' ');
  if (scheme !== 'Bearer' || !token) {
    return res.status(401).json({ error: 'missing bearer token' });
  }
  try {
    const claims = jwt.verify(token, config.jwtSecret, { algorithms: ['HS256'] });
    req.user = { id: Number(claims.sub), role: claims.role };
    return next();
  } catch {
    return res.status(401).json({ error: 'invalid or expired token' });
  }
}

function requireRole(role) {
  return (req, res, next) => {
    if (req.user.role !== role) return res.status(403).json({ error: `${role} role required` });
    return next();
  };
}

async function loadCourier(req, res, next) {
  if (req.user.role !== 'courier') return res.status(403).json({ error: 'couriers only' });
  const courier = await courierRepo.findByUserId(req.user.id);
  if (!courier) return res.status(404).json({ error: 'courier profile not found' });
  req.courier = courier;
  return next();
}

async function requirePartnerKey(req, res, next) {
  const raw = req.get('x-partner-key');
  if (!raw || !raw.includes('.')) return res.status(401).json({ error: 'missing partner key' });
  const key = await courierRepo.findPartnerKey(raw.split('.')[0]);
  if (!key || !apiKeyHashMatches(sha256Hex(raw), key.key_hash)) {
    return res.status(401).json({ error: 'invalid partner key' });
  }
  req.partner = { restaurantId: key.restaurant_id };
  return next();
}

module.exports = { authenticate, requireRole, loadCourier, requirePartnerKey };
