'use strict';

/**
 * Chooses which delivery profile the statement-delivery feature runs under.
 *
 * The `test` profile carries throwaway signing material so the unit suite can
 * run without a populated environment. It must never be the active profile of
 * a running service, so selection is driven purely by NODE_ENV and the test
 * profile is only reachable when NODE_ENV is exactly `test`. Any attempt to
 * request it explicitly from another environment is rejected.
 */

const KNOWN_PROFILES = new Set(['test', 'development', 'production']);

class ProfileSelectionError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ProfileSelectionError';
  }
}

/**
 * Resolve the active profile name from the environment.
 *
 *   NODE_ENV=test        -> test
 *   NODE_ENV=development -> development (also the default when unset)
 *   anything else        -> production
 *
 * DELIVERY_PROFILE may narrow the choice but can never select `test` unless
 * NODE_ENV is also `test`.
 */
function selectProfileName(environment = process.env) {
  const nodeEnv = environment.NODE_ENV || 'development';
  const requested = environment.DELIVERY_PROFILE;

  if (requested !== undefined && requested !== '') {
    if (!KNOWN_PROFILES.has(requested)) {
      throw new ProfileSelectionError(`unknown delivery profile: ${requested}`);
    }
    if (requested === 'test' && nodeEnv !== 'test') {
      throw new ProfileSelectionError('the test delivery profile is only available under NODE_ENV=test');
    }
    return requested;
  }

  if (nodeEnv === 'test') {
    return 'test';
  }
  if (nodeEnv === 'development') {
    return 'development';
  }
  return 'production';
}

/**
 * Load the profile definitions lazily and return the one matching the current
 * environment. The test profile module is only required when it is selected.
 */
function loadActiveProfile(environment = process.env) {
  const name = selectProfileName(environment);
  // eslint-disable-next-line global-require
  const profiles = require('./profiles');
  const factory = profiles[name];
  if (typeof factory !== 'function') {
    throw new ProfileSelectionError(`no factory registered for profile: ${name}`);
  }
  const profile = factory(environment);
  if (name !== 'test' && profile.webhookSigningKey === profiles.TEST_ONLY_WEBHOOK_KEY) {
    throw new ProfileSelectionError('test signing material leaked into a non-test profile');
  }
  return Object.freeze({ name, ...profile });
}

module.exports = { selectProfileName, loadActiveProfile, ProfileSelectionError, KNOWN_PROFILES };
