'use strict';

const crypto = require('crypto');
const express = require('express');
const { toSameOriginPath } = require('../guards/returnTo');
const { completeSso } = require('../handlers/ssoCallback');

const router = express.Router();

/**
 * Begin a servicing SSO hand-off from the partner portal. The requested
 * post-login destination is reduced to a same-origin path before it is
 * stored in the session; this is the only place that key is written.
 */
router.get('/start', (req, res) => {
  const state = crypto.randomBytes(16).toString('hex');
  req.session.part2SsoState = state;
  req.session.part2PostLogin = toSameOriginPath(req.query.next, '/servicing');
  const idp = new URL('/oauth2/authorize', process.env.PART2_IDP_URL || 'https://idp.examplelending.test');
  idp.searchParams.set('client_id', process.env.PART2_IDP_CLIENT_ID || 'servicing-portal');
  idp.searchParams.set('response_type', 'code');
  idp.searchParams.set('state', state);
  return res.redirect(idp.toString());
});

router.get('/callback', completeSso);

module.exports = router;
