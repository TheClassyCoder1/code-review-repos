'use strict';

const Redis = require('ioredis');
const config = require('../config');
const { createHttpClient } = require('../lib/http');

const redis = new Redis(config.redisUrl);
const routing = createHttpClient({
  baseUrl: config.routing.baseUrl,
  timeoutMs: config.routing.timeoutMs,
});

const cacheKey = (courierId) => `route-plan:courier:${courierId}`;

async function fetchRoutePlan(courierId, origin, stops) {
  const cached = await redis.get(cacheKey(courierId));
  if (cached) return JSON.parse(cached);
  const plan = await routing.post('/v2/plan', {
    origin: { lat: origin.lat, lng: origin.lng },
    stops: stops.map((s) => ({ id: s.id, lat: s.lat, lng: s.lng })),
  });
  await redis.set(cacheKey(courierId), JSON.stringify(plan), 'EX', config.routeCacheTtlSec);
  return plan;
}

function summarizePlan(plan) {
  return {
    legs: plan.legs.length,
    distanceKm: plan.distance_m / 1000,
    durationMin: Math.ceil(plan.duration_s / 60),
  };
}

async function invalidateRoutePlan(courierId) {
  try {
    await redis.del(cacheKey(courierId));
  } catch (err) {
    console.warn(JSON.stringify({ msg: 'route plan cache invalidation failed', courierId, error: err.message }));
  }
  return { invalidated: true };
}

module.exports = { fetchRoutePlan, summarizePlan, invalidateRoutePlan };
