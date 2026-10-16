'use strict';

const { pricing } = require('../config');

function toCents(amount) {
  if (typeof amount !== 'string' || !/^\d{1,7}(\.\d{1,2})?$/.test(amount)) {
    throw new TypeError('amount must be a decimal string such as "12.50"');
  }
  const [whole, fraction = ''] = amount.split('.');
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
}

function formatCents(cents) {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  return `${sign}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

function courierFeeCents(distanceKm) {
  if (!Number.isFinite(distanceKm) || distanceKm < 0) {
    throw new RangeError('distanceKm must be a non-negative number');
  }
  const variable = pricing.perKmCents * distanceKm;
  return Math.round(pricing.baseFeeCents + variable);
}

module.exports = { toCents, formatCents, courierFeeCents };
