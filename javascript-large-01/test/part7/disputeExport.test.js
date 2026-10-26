'use strict';

const test = require('node:test');
const assert = require('node:assert');
const disputeExport = require('../../src/part7/disputeExport');

const base = {
  id: 'dsp-1',
  statementId: 'stmt-1',
  period: '2026-10',
  reasonCode: 'INCORRECT_AMOUNT',
  amount: 12.5,
  status: 'OPEN',
  openedAt: Date.UTC(2026, 10, 1),
  respondBy: Date.UTC(2026, 11, 1),
  reference: null,
};

test('csvCell quotes separators and neutralises formulas', () => {
  assert.strictEqual(disputeExport.csvCell('plain'), 'plain');
  assert.strictEqual(disputeExport.csvCell('a,b'), '"a,b"');
  assert.strictEqual(disputeExport.csvCell('say "hi"'), '"say ""hi"""');
  assert.strictEqual(disputeExport.csvCell('=SUM(A1)'), "'=SUM(A1)");
  assert.strictEqual(disputeExport.csvCell(null), '');
});

test('masked reference keeps its length', () => {
  assert.strictEqual(disputeExport.maskReference(''), '');
  assert.strictEqual(disputeExport.maskReference('123'), '***');
  assert.strictEqual(disputeExport.maskReference('PAY 0099 1234').length, 11);
});

test('toCsv writes a header and one row per dispute', () => {
  const csv = disputeExport.toCsv([base, { ...base, id: 'dsp-2' }], Date.UTC(2026, 10, 2));
  const lines = csv.trim().split('\n');
  assert.strictEqual(lines.length, 3);
  assert.strictEqual(lines[0], disputeExport.COLUMNS.join(','));
  assert.ok(lines[1].startsWith('dsp-1,stmt-1,2026-10,INCORRECT_AMOUNT,12.50,OPEN,'));
  assert.ok(lines[1].endsWith(',N,'));
});

test('summarise totals amounts per reason code', () => {
  const summary = disputeExport.summarise([
    base,
    { ...base, amount: 7.25 },
    { ...base, reasonCode: 'DUPLICATE_CHARGE', amount: 3 },
    { ...base, reasonCode: 'UNKNOWN', amount: 100 },
  ]);
  assert.deepStrictEqual(summary.INCORRECT_AMOUNT, { count: 2, amount: 19.75 });
  assert.deepStrictEqual(summary.DUPLICATE_CHARGE, { count: 1, amount: 3 });
  assert.deepStrictEqual(summary.PAYMENT_NOT_APPLIED, { count: 0, amount: 0 });
});
