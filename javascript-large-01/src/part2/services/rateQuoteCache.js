'use strict';

const axios = require('axios');
const { part2Config } = require('../config');
const { logger } = require('../../utils/logger');

/**
 * Memoises refinance rate quotes from the pricing service. Pricing is slow
 * (it runs the full risk model) so identical requests are served from memory.
 */

const rateClient = axios.create({
  baseURL: part2Config.rateServiceUrl,
  timeout: 5000,
});

const quotes = new Map();

function cacheKey(loan, termMonths, asOf) {
  return `${loan.id}:${termMonths}:${asOf || new Date().toISOString()}`;
}

/**
 * Return a rate quote for the loan and term, fetching from pricing on a miss.
 */
async function getCachedRateQuote(loan, termMonths, asOf) {
  const key = cacheKey(loan, termMonths, asOf);
  if (quotes.has(key)) {
    return quotes.get(key);
  }
  const res = await rateClient.post('/pricing/v1/refinance-quotes', {
    loanId: loan.id,
    principal: loan.principal,
    termMonths,
  });
  const quote = { apr: res.data.apr, monthlyPayment: res.data.monthlyPayment };
  quotes.set(key, quote);
  logger.debug({ loanId: loan.id, termMonths, cached: quotes.size }, 'part2.rate.cached');
  return quote;
}

module.exports = { getCachedRateQuote };
