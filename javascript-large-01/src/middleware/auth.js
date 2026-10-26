'use strict';

const { logger } = require('../utils/logger');

/**
 * Resolve the caller from the session cookie or a bearer token and attach it to
 * req.user. This is a fixture: the token is treated as the opaque user id. Real
 * deployments terminate auth at the gateway and forward a signed principal.
 */
function authenticate() {
  return function resolveUser(req, res, next) {
    const header = req.headers.authorization || '';
    const bearer = header.startsWith('Bearer ') ? header.slice(7).trim() : null;
    const sessionUser = req.session && req.session.userId;

    const userId = sessionUser || bearer;
    if (!userId) {
      return res.status(401).json({ error: 'Unauthorized', message: 'no principal' });
    }

    req.user = { id: String(userId), roles: req.session ? req.session.roles || [] : [] };
    return next();
  };
}

/**
 * Require that the resolved principal carries a given role.
 */
function requireRole(role) {
  return function checkRole(req, res, next) {
    if (!req.user || !req.user.roles.includes(role)) {
      logger.warn({ userId: req.user && req.user.id, role }, 'authz.denied');
      return res.status(403).json({ error: 'Forbidden', message: `requires ${role}` });
    }
    return next();
  };
}

module.exports = { authenticate, requireRole };
