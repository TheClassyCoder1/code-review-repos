'use strict';

const MINUTES_PER_HOUR = 60;
const MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;

function dailyCap(card, minutes) {
  if (!Number.isInteger(card.daily_max_cents)) {
    return Number.POSITIVE_INFINITY;
  }
  const days = Math.max(1, Math.ceil(minutes / MINUTES_PER_DAY));
  return days * card.daily_max_cents;
}

function billableHours(minutes) {
  return Math.ceil(minutes / MINUTES_PER_HOUR);
}

const DEFAULT_CURRENCY = 'USD';

function quoteFor(lot, minutes) {
  const { hourly_cents: hourly, grace_minutes: grace = 0 } = lot.rate_card;
  if (minutes <= grace) {
    return 0;
  }
  const subtotal = billableHours(minutes) * hourly;
  return Math.min(subtotal, dailyCap(lot.rate_card, minutes));
}

function formatCents(cents, currency = DEFAULT_CURRENCY) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(cents / 100);
}

function isFree(cents) {
  return cents === 0;
}

function applyDiscount(amountCents, discountPct) {
  const discounted = (amountCents * (100 - discountPct)) / 100;
  return Math.round(discounted);
}

module.exports = { dailyCap, billableHours, quoteFor, formatCents, isFree, applyDiscount };
