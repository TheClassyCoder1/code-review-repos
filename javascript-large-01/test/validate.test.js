'use strict';

const test = require('node:test');
const assert = require('node:assert');
const {
  assertSafeId,
  assertPageNumber,
  assertFormat,
  ValidationError,
} = require('../src/utils/validate');

test('assertSafeId accepts bundle-style identifiers', () => {
  assert.strictEqual(assertSafeId('stmt-1001'), 'stmt-1001');
  assert.strictEqual(assertSafeId('bundle_2026_09'), 'bundle_2026_09');
});

test('assertSafeId rejects shell metacharacters', () => {
  for (const bad of ['a;b', 'a b', 'a$(x)', '../x', 'a|b', 'a`b`']) {
    assert.throws(() => assertSafeId(bad), ValidationError);
  }
});

test('assertPageNumber accepts short digit runs only', () => {
  assert.strictEqual(assertPageNumber(3), '3');
  assert.throws(() => assertPageNumber('3; rm -rf /'), ValidationError);
  assert.throws(() => assertPageNumber('-1'), ValidationError);
});

test('assertFormat enforces the allow-list', () => {
  assert.strictEqual(assertFormat('csv'), 'csv');
  assert.throws(() => assertFormat('csv; cat /etc/passwd'), ValidationError);
});
