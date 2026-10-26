'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { redactHeaders, redactBody } = require('../src/utils/redact');

test('redactHeaders masks credential-bearing headers case-insensitively', () => {
  const out = redactHeaders({
    Authorization: 'Bearer abc',
    Cookie: 'stmt_session=xyz',
    'X-Partner-Key': 'secret',
    'content-type': 'application/json',
  });
  assert.strictEqual(out.Authorization, '[redacted]');
  assert.strictEqual(out.Cookie, '[redacted]');
  assert.strictEqual(out['X-Partner-Key'], '[redacted]');
  assert.strictEqual(out['content-type'], 'application/json');
});

test('redactBody masks known sensitive fields recursively', () => {
  const out = redactBody({ email: 'a@b.c', password: 'hunter2', nested: { token: 't' } });
  assert.strictEqual(out.email, 'a@b.c');
  assert.strictEqual(out.password, '[redacted]');
  assert.strictEqual(out.nested.token, '[redacted]');
});
