'use strict';

const express = require('express');
const { authenticate } = require('../../middleware/auth');
const { requireLoanOwnership } = require('../guards/loanOwnership');
const { getPayoffQuote } = require('../services/payoffService');
const { getCachedRateQuote } = require('../services/rateQuoteCache');

const router = express.Router();

/**
 * Current payoff quote for one of the caller's loans.
 */
router.get('/:loanId', authenticate(), requireLoanOwnership(), (req, res, next) => {
  try {
    const quote = getPayoffQuote(req.params.loanId);
    return res.json({ quote });
  } catch (err) {
    return next(err);
  }
});

/**
 * Refinance teaser: what the borrower's rate would be if they refinanced
 * today with the requested term.
 */
router.get('/:loanId/refinance', authenticate(), requireLoanOwnership(), async (req, res, next) => {
  try {
    const termMonths = Number(req.query.termMonths || 36);
    if (![24, 36, 48, 60].includes(termMonths)) {
      return res.status(400).json({ error: 'BadRequest', message: 'unsupported term' });
    }
    const rate = await getCachedRateQuote(req.loan, termMonths, req.query.asOf);
    return res.json({ loanId: req.loan.id, termMonths, rate });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
