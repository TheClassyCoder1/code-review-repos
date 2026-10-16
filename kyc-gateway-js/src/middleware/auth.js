'use strict';

const jwt = require('jsonwebtoken');
const config = require('../config');

function requireApiKey(req, res, next) {
  const header = req.get('authorization') || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ error: 'missing token' });
  try {
    req.principal = jwt.verify(token, config.jwtSecret, { algorithms: ['HS256'] });
    return next();
  } catch (err) {
    return res.status(401).json({ error: 'invalid token' });
  }
}

function requireAdmin(req, res, next) {
  if (!req.principal || req.principal.role !== 'admin') {
    return res.status(403).json({ error: 'admin only' });
  }
  return next();
}

module.exports = { requireApiKey, requireAdmin };
