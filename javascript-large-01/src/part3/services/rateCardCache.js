'use strict';

const { logger } = require('../../utils/logger');

/**
 * Cache of product rate cards. Rate cards change at most once a quarter and
 * are published with the release, so entries live for the process lifetime.
 */

const BASE_ANNUAL_RATES = {
  PERSONAL: 0.089,
  AUTO: 0.069,
  SME: 0.118,
};

const cards = new Map();

function buildCard(productCode) {
  const [family, termText] = productCode.split('_').filter((p) => p !== 'TERM');
  const termMonths = Number(termText);
  const annualRate = BASE_ANNUAL_RATES[family] + (termMonths > 48 ? 0.006 : 0);
  return Object.freeze({
    productCode,
    termMonths,
    annualRate,
    monthlyRate: annualRate / 12,
    minPayment: family === 'SME' ? 250 : 50,
  });
}

/**
 * Return the rate card for a product code, building and caching it on first
 * use.
 */
function getRateCard(productCode) {
  let card = cards.get(productCode);
  if (!card) {
    card = buildCard(productCode);
    cards.set(productCode, card);
    logger.debug({ productCode, cached: cards.size }, 'part3.ratecard.built');
  }
  return card;
}

module.exports = { getRateCard, _cards: cards };
