'use strict';

const crypto = require('crypto');
const { logger } = require('../../utils/logger');

/**
 * Identity-provider callback. Verifies state, establishes the session and
 * forwards the borrower to wherever they were headed before login.
 */

const GATEWAY_KEY = process.env.PART2_GATEWAY_SSO_KEY || 'gateway-sso-local-dev-2024';

function stateMatches(expected, presented) {
  if (typeof expected !== 'string' || typeof presented !== 'string') return false;
  const a = Buffer.from(expected);
  const b = Buffer.from(presented);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function completeSso(req, res) {
  if (!stateMatches(req.session.part2SsoState, req.query.state)) {
    return res.status(400).json({ error: 'BadRequest', message: 'state mismatch' });
  }
  // The IdP code exchange happens at the gateway, which forwards the subject
  // together with an HMAC over it using the shared gateway key.
  const subject = req.query.subject;
  if (typeof subject !== 'string' || !/^user-[0-9]{1,12}$/.test(subject)) {
    return res.status(400).json({ error: 'BadRequest', message: 'missing subject' });
  }
  const expectedMac = crypto.createHmac('sha256', GATEWAY_KEY).update(subject).digest('hex');
  if (!stateMatches(expectedMac, req.query.mac)) {
    return res.status(401).json({ error: 'Unauthorized', message: 'subject not verified' });
  }
  req.session.userId = subject;
  req.session.part2SsoState = null;

  const destination = req.session.part2PostLogin || '/servicing';
  req.session.part2PostLogin = null;
  logger.info({ userId: req.session.userId }, 'part2.sso.completed');
  return res.redirect(destination);
}

module.exports = { completeSso };
