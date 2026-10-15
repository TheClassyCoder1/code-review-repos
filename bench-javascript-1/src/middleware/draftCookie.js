'use strict';

const crypto = require('crypto');
const config = require('../config');

const COOKIE_NAME = 'intake_draft';

function sign(body) {
  return crypto.createHmac('sha256', config.draftCookieSecret).update(body).digest('base64url');
}

function encodeDraftCookie(payload) {
  const body = payload.toString('base64url');
  return `${body}.${sign(body)}`;
}

function draftCookie(req, res, next) {
  const raw = req.cookies ? req.cookies[COOKIE_NAME] : undefined;
  if (!raw) {
    return next();
  }
  const [body, signature] = raw.split('.');
  if (!body || !signature) {
    return next();
  }
  const expected = Buffer.from(sign(body));
  const actual = Buffer.from(signature);
  if (expected.length === actual.length && crypto.timingSafeEqual(expected, actual)) {
    req.intakeDraftPayload = Buffer.from(body, 'base64url');
  }
  return next();
}

module.exports = { draftCookie, encodeDraftCookie, COOKIE_NAME };
