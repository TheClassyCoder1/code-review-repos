'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { maskEmail, maskPhone } = require('../../src/part6/utils/contactMask');
const { toInternalIdEvent } = require('../../src/part6/analytics/eventSchema');

test('maskEmail hides the local part and domain name', () => {
  assert.strictEqual(maskEmail('jane.doe@example.com'), 'j*******@*******.com');
  assert.strictEqual(maskEmail(''), '');
  assert.strictEqual(maskEmail(undefined), '');
});

test('maskPhone keeps only the last two digits', () => {
  assert.strictEqual(maskPhone('+44 7700 900123'), '**********23');
  assert.strictEqual(maskPhone('12'), '**');
});

test('usage events drop everything outside the id allowlist', () => {
  const event = toInternalIdEvent('statement.viewed', {
    userId: 'user-7',
    statementId: 'stmt-1001',
    email: 'jane.doe@example.com',
    fullName: 'Jane Doe',
  });
  assert.deepStrictEqual(Object.keys(event.properties).sort(), ['statementId', 'userId']);
});

test('usage events reject free-text values in id fields', () => {
  const event = toInternalIdEvent('statement.viewed', {
    userId: 'user-7',
    statementId: 'jane.doe@example.com',
  });
  assert.strictEqual(event.properties.statementId, undefined);
});
