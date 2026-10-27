'use strict';

const express = require('express');
const { receiptPage } = require('../views/receiptPage');

const router = express.Router();

/**
 * The payment processor redirects the borrower here with ?code=...&conf=...
 * Neither value is reflected verbatim: `code` selects one of the fixed status
 * entries below (unknown codes fall back to `unknown`), and `conf` must be a
 * processor confirmation number of 8–20 upper-case alphanumerics.
 */
const STATUS_CATALOG = Object.freeze({
  approved: {
    tone: 'ok',
    heading: 'Payment received',
    message: 'Your payment was accepted and will post within one business day.',
  },
  declined: {
    tone: 'warn',
    heading: 'Payment declined',
    message: 'Your bank declined this payment. Please try another method.',
  },
  unknown: {
    tone: 'info',
    heading: 'Payment submitted',
    message: 'We could not confirm the status yet. Check your account later.',
  },
});

const CONFIRMATION = /^[A-Z0-9]{8,20}$/;

router.get('/payment-result', (req, res) => {
  const code = typeof req.query.code === 'string' ? req.query.code : 'unknown';
  const status = Object.prototype.hasOwnProperty.call(STATUS_CATALOG, code)
    ? STATUS_CATALOG[code]
    : STATUS_CATALOG.unknown;
  const conf = typeof req.query.conf === 'string' && CONFIRMATION.test(req.query.conf)
    ? req.query.conf
    : 'unavailable';
  res.setHeader('content-type', 'text/html; charset=utf-8');
  return res.send(receiptPage(status, conf));
});

module.exports = router;
