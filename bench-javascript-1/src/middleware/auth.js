'use strict';

const jwt = require('jsonwebtoken');
const config = require('../config');

function readBearer(req) {
  const header = req.get('authorization') || '';
  if (header.startsWith('Bearer ')) {
    return header.slice(7);
  }
  return req.cookies ? req.cookies.session : undefined;
}

function requireAuth(req, res, next) {
  const token = readBearer(req);
  if (!token) {
    return res.status(401).json({ error: 'unauthenticated' });
  }
  try {
    const claims = jwt.verify(token, config.jwtSecret, { issuer: 'clinic-booking', algorithms: ['HS256'] });
    req.user = { id: Number(claims.sub), role: claims.role, patientId: claims.patientId };
    return next();
  } catch (err) {
    return res.status(401).json({ error: 'invalid_token' });
  }
}

function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'forbidden' });
    }
    return next();
  };
}

module.exports = { requireAuth, requireRole };
