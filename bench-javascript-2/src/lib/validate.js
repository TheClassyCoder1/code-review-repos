'use strict';

const config = require('../config');

function httpError(status, message) {
  const err = new Error(message);
  err.status = status;
  return err;
}

const isLatitude = (v) => typeof v === 'number' && Number.isFinite(v) && v >= -90 && v <= 90;
const isLongitude = (v) => typeof v === 'number' && Number.isFinite(v) && v >= -180 && v <= 180;
const isPositiveInt = (v) => Number.isInteger(v) && v > 0;

function validateOrderInput(body) {
  if (!body || typeof body !== 'object') throw httpError(400, 'body must be a JSON object');
  const { restaurantId, items, dropoff } = body;
  const errors = [];
  if (!isPositiveInt(restaurantId)) errors.push('restaurantId must be a positive integer');
  if (!Array.isArray(items) || items.length === 0 || items.length > 50) {
    errors.push('items must contain between 1 and 50 entries');
  } else {
    items.forEach((item, i) => {
      if (!item || !isPositiveInt(item.menuItemId)) errors.push(`items[${i}].menuItemId is invalid`);
      else if (!Number.isInteger(item.quantity) || item.quantity < 1 || item.quantity > 20) {
        errors.push(`items[${i}].quantity must be between 1 and 20`);
      }
    });
  }
  if (!dropoff || !isLatitude(dropoff.lat) || !isLongitude(dropoff.lng)) {
    errors.push('dropoff must contain a valid lat and lng');
  }
  if (errors.length) throw httpError(400, errors.join('; '));
  return {
    restaurantId,
    items: items.map(({ menuItemId, quantity }) => ({ menuItemId, quantity })),
    dropoff: { lat: dropoff.lat, lng: dropoff.lng },
  };
}

function parseSlotHour(value) {
  const hour = Number(value);
  if (!Number.isInteger(hour) || hour < 0 || hour > 23) {
    throw httpError(400, 'hour must be an integer between 0 and 23');
  }
  return hour;
}

function parsePage(query) {
  const page = query.page === undefined ? 1 : Number(query.page);
  const size = query.size === undefined ? 20 : Number(query.size);
  if (!isPositiveInt(page)) throw httpError(400, 'page must be a positive integer');
  if (!isPositiveInt(size) || size > config.maxPageSize) {
    throw httpError(400, `size must be between 1 and ${config.maxPageSize}`);
  }
  return { limit: size, offset: (page - 1) * size };
}

module.exports = { httpError, isLatitude, isLongitude, isPositiveInt, validateOrderInput, parseSlotHour, parsePage };
