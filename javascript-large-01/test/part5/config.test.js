'use strict';

const test = require('node:test');
const assert = require('node:assert');

const { selectProfileName, loadActiveProfile, ProfileSelectionError } = require('../../src/part5/config/profileSelector');
const { assertNoPlaceholderSecrets, PlaceholderSecretError } = require('../../src/part5/config/secretGuard');
const { FIXTURE_CREDENTIALS } = require('./fixtures/deliveryFixtures');

test('profile selection follows NODE_ENV', () => {
  assert.strictEqual(selectProfileName({ NODE_ENV: 'test' }), 'test');
  assert.strictEqual(selectProfileName({}), 'development');
  assert.strictEqual(selectProfileName({ NODE_ENV: 'staging' }), 'production');
});

test('the test profile cannot be forced outside NODE_ENV=test', () => {
  assert.throws(() => selectProfileName({ NODE_ENV: 'production', DELIVERY_PROFILE: 'test' }), ProfileSelectionError);
  assert.throws(() => selectProfileName({ DELIVERY_PROFILE: 'qa' }), ProfileSelectionError);
});

test('production profile requires a signing key', () => {
  assert.throws(() => loadActiveProfile({ NODE_ENV: 'production' }), /DELIVERY_WEBHOOK_SIGNING_KEY/);
  const profile = loadActiveProfile({ NODE_ENV: 'production', DELIVERY_WEBHOOK_SIGNING_KEY: 'k-from-vault' });
  assert.strictEqual(profile.name, 'production');
  assert.strictEqual(profile.dryRun, false);
});

test('placeholder credentials are rejected outside the test environment', () => {
  assert.doesNotThrow(() => assertNoPlaceholderSecrets(FIXTURE_CREDENTIALS, { NODE_ENV: 'test' }));
  assert.throws(() => assertNoPlaceholderSecrets(FIXTURE_CREDENTIALS, { NODE_ENV: 'production' }), PlaceholderSecretError);
  assert.doesNotThrow(() => assertNoPlaceholderSecrets({ KEY: 'k-from-vault' }, { NODE_ENV: 'production' }));
});
