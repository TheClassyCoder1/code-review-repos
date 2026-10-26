'use strict';

const loanAccounts = require('../models/loanAccounts');
const { logger } = require('../../utils/logger');

/**
 * Require that req.params.loanId belongs to the authenticated principal.
 * Responds 404 (not 403) on mismatch so loan ids cannot be enumerated.
 * Servicing agents with a servicing role may view any loan.
 */
const AGENT_ROLES = ['servicing:read', 'servicing:write'];

function requireLoanOwnership() {
  return function checkLoanOwner(req, res, next) {
    const loanId = req.params.loanId;
    const loan = loanAccounts.findById(loanId);
    if (!loan) {
      return res.status(404).json({ error: 'NotFound', message: 'no such loan' });
    }
    const isAgent =
      req.user && AGENT_ROLES.some((role) => req.user.roles.includes(role));
    if (!isAgent && (!req.user || loan.borrowerId !== req.user.id)) {
      logger.warn({ userId: req.user && req.user.id, loanId }, 'part2.loan.ownership.denied');
      return res.status(404).json({ error: 'NotFound', message: 'no such loan' });
    }
    req.loan = loan;
    return next();
  };
}

module.exports = { requireLoanOwnership };
