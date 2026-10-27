'use strict';

/**
 * Delivery window arithmetic. Statements are only pushed to customers inside
 * a configured daily window (UTC hours), optionally on weekdays only, and never
 * inside the customer's quiet hours. All timestamps are epoch milliseconds and
 * all hours are UTC so the dispatcher behaves identically on every host.
 */

const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;

class DeliveryWindowError extends Error {
  constructor(message) {
    super(message);
    this.name = 'DeliveryWindowError';
  }
}

function assertHour(value, label) {
  if (!Number.isInteger(value) || value < 0 || value > 24) {
    throw new DeliveryWindowError(`${label} must be an integer hour between 0 and 24`);
  }
}

function isWeekend(date) {
  const day = date.getUTCDay();
  return day === 0 || day === 6;
}

function startOfUtcDay(ms) {
  return ms - (((ms % DAY_MS) + DAY_MS) % DAY_MS);
}

/**
 * Return the earliest timestamp at or after `fromMs` that falls inside the
 * delivery window. The window is [startHour, endHour) in UTC. When
 * `weekdaysOnly` is set, Saturdays and Sundays are skipped entirely.
 */
function nextDeliverySlot(fromMs, window) {
  const { startHour, endHour, weekdaysOnly = false } = window;
  assertHour(startHour, 'startHour');
  assertHour(endHour, 'endHour');
  if (startHour >= endHour) {
    throw new DeliveryWindowError('startHour must be before endHour');
  }

  let dayStart = startOfUtcDay(fromMs);
  // A window can be at most a week away when weekdaysOnly is set.
  for (let i = 0; i < 8; i += 1) {
    const opens = dayStart + startHour * HOUR_MS;
    const closes = dayStart + endHour * HOUR_MS;
    const skipDay = weekdaysOnly && isWeekend(new Date(dayStart));

    if (!skipDay && fromMs < closes) {
      return Math.max(fromMs, opens);
    }
    dayStart += DAY_MS;
  }
  throw new DeliveryWindowError('no delivery slot found within a week');
}

/**
 * True when the timestamp falls inside the customer's quiet hours. Quiet hours
 * may wrap midnight (e.g. 22 -> 7), in which case the range is the union of
 * [startHour, 24) and [0, endHour). Equal bounds mean no quiet hours.
 */
function isWithinQuietHours(timestampMs, quiet) {
  if (!quiet) {
    return false;
  }
  const { startHour, endHour } = quiet;
  assertHour(startHour, 'quiet.startHour');
  assertHour(endHour, 'quiet.endHour');
  if (startHour === endHour) {
    return false;
  }

  const hour = new Date(timestampMs).getUTCHours();
  if (startHour < endHour) {
    return hour >= startHour && hour < endHour;
  }
  return hour >= startHour || hour < endHour;
}

/**
 * Combine the delivery window with quiet hours: step forward an hour at a time
 * from the next window slot until the slot is outside quiet hours.
 */
function resolveSendTime(fromMs, window, quiet) {
  let candidate = nextDeliverySlot(fromMs, window);
  for (let i = 0; i < 24 * 8; i += 1) {
    if (!isWithinQuietHours(candidate, quiet)) {
      return candidate;
    }
    const nextHour = startOfUtcDay(candidate) + (new Date(candidate).getUTCHours() + 1) * HOUR_MS;
    candidate = nextDeliverySlot(nextHour, window);
  }
  throw new DeliveryWindowError('delivery window is fully covered by quiet hours');
}

module.exports = {
  nextDeliverySlot,
  isWithinQuietHours,
  resolveSendTime,
  DeliveryWindowError,
  HOUR_MS,
  DAY_MS,
};
