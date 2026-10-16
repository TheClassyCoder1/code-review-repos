'use strict';

const rateCard = {
  type: 'object',
  required: ['hourly_cents'],
  properties: {
    hourly_cents: { type: 'integer', minimum: 0 },
    grace_minutes: { type: 'integer', minimum: 0, maximum: 60 },
    daily_max_cents: { type: 'integer', minimum: 0 },
  },
};

const createLotBody = {
  type: 'object',
  required: ['name', 'city', 'capacity'],
  properties: {
    name: { type: 'string', minLength: 1, maxLength: 120 },
    city: { type: 'string', minLength: 1, maxLength: 80 },
    capacity: { type: 'integer', minimum: 1, maximum: 20000 },
    rateCard,
  },
  additionalProperties: false,
};

const reserveBayBody = {
  type: 'object',
  required: ['bayCode', 'plate'],
  properties: {
    bayCode: { type: 'string', pattern: '^[A-Z]{1,2}[0-9]{1,4}$' },
    plate: { type: 'string', minLength: 2, maxLength: 12 },
  },
};

const listPermitsQuery = {
  type: 'object',
  properties: {
    page: { type: 'integer', minimum: 0, default: 0 },
    pageSize: { type: 'integer', minimum: 1, maximum: 100, default: 25 },
    zone: { type: 'string', maxLength: 32 },
  },
  additionalProperties: false,
};

const createPermitBody = {
  type: 'object',
  required: ['zoneId', 'plate', 'holder', 'months'],
  properties: {
    zoneId: { type: 'integer', minimum: 1 },
    plate: { type: 'string', pattern: '^[A-Za-z0-9 -]{2,10}$' },
    months: { type: 'integer', minimum: 1, maximum: 12 },
    discountPct: { type: 'integer', minimum: 0, maximum: 100, default: 0 },
    holder: {
      type: 'object',
      required: ['name', 'email'],
      properties: {
        name: { type: 'string', minLength: 1, maxLength: 120 },
        email: { type: 'string', format: 'email', maxLength: 254 },
        phone: { type: 'string', maxLength: 20 },
      },
    },
  },
  additionalProperties: false,
};

const renewPermitBody = {
  type: 'object',
  required: ['months', 'paymentToken'],
  properties: {
    months: { type: 'integer', minimum: 1, maximum: 12 },
    paymentToken: { type: 'string', minLength: 8, maxLength: 128 },
  },
};

const payFineBody = {
  type: 'object',
  required: ['paymentToken'],
  properties: {
    paymentToken: { type: 'string', minLength: 8, maxLength: 128 },
  },
};

module.exports = {
  createLotBody,
  reserveBayBody,
  listPermitsQuery,
  createPermitBody,
  renewPermitBody,
  payFineBody,
};
