'use strict';

const express = require('express');
const { part2Config } = require('../config');

const router = express.Router();

/**
 * End of a co-branded servicing flow: send the borrower back to the partner
 * that started it. The partner is named in the path.
 */
router.get('/:partnerId', (req, res) => {
  const partner = part2Config.partnerReturns[req.params.partnerId];
  if (!partner) {
    return res.redirect(part2Config.partnerReturns.portal.returnUrl);
  }
  return res.redirect(partner.returnUrl);
});

module.exports = router;
