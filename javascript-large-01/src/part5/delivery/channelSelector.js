'use strict';

/**
 * Picks the channel a statement notification goes out on. Customers record an
 * ordered list of channel preferences; the selector walks that list and takes
 * the first channel that is enabled and still under its daily cap.
 */

const SUPPORTED_CHANNELS = Object.freeze(['portal', 'email', 'sms']);
const DEFAULT_PREFERENCES = Object.freeze(['portal']);

class ChannelPreferenceError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ChannelPreferenceError';
    this.status = 400;
  }
}

/**
 * Normalise a raw preference list from the portal: lower-case, trim, drop
 * duplicates while keeping first-seen order, and reject unknown channels.
 * An empty or missing list falls back to portal-only delivery.
 */
function normaliseChannelPreferences(raw) {
  if (raw === undefined || raw === null) {
    return [...DEFAULT_PREFERENCES];
  }
  if (!Array.isArray(raw)) {
    throw new ChannelPreferenceError('channel preferences must be an array');
  }
  if (raw.length > SUPPORTED_CHANNELS.length * 2) {
    throw new ChannelPreferenceError('too many channel preferences');
  }

  const seen = new Set();
  const out = [];
  for (const entry of raw) {
    if (typeof entry !== 'string') {
      throw new ChannelPreferenceError('channel preference entries must be strings');
    }
    const channel = entry.trim().toLowerCase();
    if (!SUPPORTED_CHANNELS.includes(channel)) {
      throw new ChannelPreferenceError(`unsupported channel: ${channel}`);
    }
    if (!seen.has(channel)) {
      seen.add(channel);
      out.push(channel);
    }
  }
  return out.length > 0 ? out : [...DEFAULT_PREFERENCES];
}

/**
 * Choose the delivery channel for one notification.
 *
 * @param {string[]} preferences normalised, ordered channel list
 * @param {object} channelSettings map of channel -> { enabled, dailyCap }
 * @param {object} sentToday map of channel -> count already sent today
 * @returns {string|null} the chosen channel, or null when every option is
 *   disabled or capped (the caller defers the job to tomorrow)
 */
function selectChannel(preferences, channelSettings, sentToday = {}) {
  if (!Array.isArray(preferences) || preferences.length === 0) {
    return null;
  }
  for (const channel of preferences) {
    const settings = channelSettings[channel];
    if (!settings || !settings.enabled) {
      continue;
    }
    const used = Number(sentToday[channel] || 0);
    if (used < settings.dailyCap) {
      return channel;
    }
  }
  return null;
}

module.exports = {
  normaliseChannelPreferences,
  selectChannel,
  ChannelPreferenceError,
  SUPPORTED_CHANNELS,
};
