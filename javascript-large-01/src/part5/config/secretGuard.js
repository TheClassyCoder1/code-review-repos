'use strict';

/**
 * Boot-time check for delivery credentials. Fixture and example files use
 * obvious placeholder values; this guard makes sure none of those values can
 * be used by a running service. It is called by the delivery bootstrap before
 * any dispatcher is started.
 */

const PLACEHOLDER_VALUES = new Set(['changeme', 'change-me', 'placeholder', 'example', 'dummy', 'secret']);

class PlaceholderSecretError extends Error {
  constructor(names) {
    super(`refusing to start: placeholder credentials configured for ${names.join(', ')}`);
    this.name = 'PlaceholderSecretError';
    this.fields = names;
  }
}

function isPlaceholder(value) {
  if (typeof value !== 'string') {
    return false;
  }
  return PLACEHOLDER_VALUES.has(value.trim().toLowerCase());
}

/**
 * Reject any credential whose value is a known placeholder. Under NODE_ENV=test
 * the check is skipped so fixtures can be loaded by the unit suite.
 */
function assertNoPlaceholderSecrets(credentials, environment = process.env) {
  if (environment.NODE_ENV === 'test') {
    return;
  }
  const offending = Object.entries(credentials || {})
    .filter(([, value]) => isPlaceholder(value))
    .map(([name]) => name);
  if (offending.length > 0) {
    throw new PlaceholderSecretError(offending);
  }
}

module.exports = { assertNoPlaceholderSecrets, isPlaceholder, PlaceholderSecretError, PLACEHOLDER_VALUES };
