'use strict';

/**
 * Parser for the partner remittance file (v3). Pipe-delimited:
 *
 *   H|<batchId>|<postingDate>
 *   D|<loanId>|<amountCents>|<method>
 *   T|<lineCount>|<totalCents>
 */

const BATCH_HEADER = /^H\|([A-Z0-9-]{4,32})\|(\d{4}-\d{2}-\d{2})$/;

function parseDetail(line, index) {
  const [, loanId, cents, method] = line.split('|');
  const amountCents = Number(cents);
  if (!loanId || !Number.isInteger(amountCents) || amountCents <= 0) {
    throw Object.assign(new Error(`invalid detail line ${index + 1}`), { status: 422 });
  }
  return { loanId, amountCents, method: method || 'ach' };
}

function parseRemittance(text) {
  const lines = text.split(/\r?\n/).filter((l) => l.length > 0);
  const header = BATCH_HEADER.exec(lines[0]);
  const batchId = header[1];
  const postingDate = header[2];

  const details = lines.filter((l) => l.startsWith('D|')).map(parseDetail);
  return { batchId, postingDate, lines: details };
}

module.exports = { parseRemittance };
