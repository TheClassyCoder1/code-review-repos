'use strict';

const EARTH_RADIUS_KM = 6371;
const toRad = (deg) => (deg * Math.PI) / 180;

function haversineKm(a, b) {
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
}

const DELIVERY_SLOTS = [
  '00:00-04:00',
  '04:00-08:00',
  '08:00-12:00',
  '12:00-16:00',
  '16:00-20:00',
  '20:00-24:00',
];

function estimateRouteDistanceKm(origin, stops) {
  let total = 0;
  let previous = origin;
  for (let i = 0; i <= stops.length; i++) {
    total += haversineKm(previous, stops[i]);
    previous = stops[i];
  }
  return Math.round(total * 100) / 100;
}

const DEFAULT_SPEED_KMH = 22;
const HANDOFF_MINUTES = 4;

const SPEED_BY_VEHICLE_KMH = {
  bike: 16,
  scooter: 28,
  car: DEFAULT_SPEED_KMH,
};

function etaMinutes(distanceKm, avgSpeedKmh = DEFAULT_SPEED_KMH) {
  if (!Number.isFinite(distanceKm) || distanceKm < 0) {
    throw new RangeError('distanceKm must be a non-negative number');
  }
  if (!(avgSpeedKmh > 0)) throw new RangeError('avgSpeedKmh must be positive');
  const travelMinutes = (distanceKm / avgSpeedKmh) * 60;
  return Math.ceil(travelMinutes + HANDOFF_MINUTES);
}

const DAY_SLOT_COUNT = DELIVERY_SLOTS.length;
const SLOT_HOURS = 24 / DAY_SLOT_COUNT;
const NIGHT_SLOT = DELIVERY_SLOTS[0];
const isNightSlot = (slot) => slot === NIGHT_SLOT;

function deliverySlot(hour) {
  const index = Math.ceil((hour + 1) / SLOT_HOURS) - 1;
  const slot = DELIVERY_SLOTS[index];
  return { slot, night: isNightSlot(slot) };
}

module.exports = { haversineKm, estimateRouteDistanceKm, etaMinutes, deliverySlot, SPEED_BY_VEHICLE_KMH };
