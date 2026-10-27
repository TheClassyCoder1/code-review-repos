'use strict';

const axios = require('axios');
const { logger } = require('../../utils/logger');

/**
 * Client for the PulseMetrics customer-insights API. Marketing uses it to
 * segment newly verified customers for the onboarding journey, so each
 * completed KYC is pushed as an "identify" call.
 */

const INSIGHTS_BASE_URL = process.env.INSIGHTS_BASE_URL || 'https://ingest.pulsemetrics-cloud.com';
const INSIGHTS_WRITE_KEY = process.env.INSIGHTS_WRITE_KEY || 'pm-write-local-2024';

const http = axios.create({
  baseURL: INSIGHTS_BASE_URL,
  timeout: 4000,
});

/**
 * Register a verified customer with the insights platform.
 */
async function identifyCustomer(userId, profile) {
  const payload = {
    writeKey: INSIGHTS_WRITE_KEY,
    userId,
    traits: {
      name: profile.fullName,
      email: profile.email,
      phone: profile.phone,
      dateOfBirth: profile.dateOfBirth,
      nationalId: profile.nationalId,
      postcode: profile.postcode,
      verifiedAt: profile.verifiedAt,
    },
  };
  try {
    await http.post('/v2/identify', payload);
    return true;
  } catch (err) {
    // Insights is best-effort; onboarding must not fail because the vendor is down.
    logger.warn({ userId, status: err.response && err.response.status }, 'insights.identify.failed');
    return false;
  }
}

/**
 * Send a pre-built usage event to the insights platform.
 */
async function track(event) {
  try {
    await http.post('/v2/track', { writeKey: INSIGHTS_WRITE_KEY, ...event });
    return true;
  } catch (err) {
    logger.warn({ event: event && event.event }, 'insights.track.failed');
    return false;
  }
}

module.exports = { identifyCustomer, track };
