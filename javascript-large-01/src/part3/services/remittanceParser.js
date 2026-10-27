'use strict';

/**
 * Parser for partner remittance files. A remittance file is a pipe-delimited
 * text document listing the payments a partner (payroll deduction provider,
 * agency) collected on our behalf:
 *
 *   # header comments start with '#'
 *   H|<partnerId>|<batchId>|<valueDate>
 *   D|<loanId>|<amount>|<partnerTxnRef>
 *   T|<lineCount>|<controlTotal>
 */

class RemittanceFormatError extends Error {
  constructor(message, lineNo) {
    super(lineNo ? `line ${lineNo}: ${message}` : message);
    this.name = 'RemittanceFormatError';
    this.status = 400;
  }
}

function parseAmount(text, lineNo) {
  if (!/^\d{1,9}(\.\d{1,2})?$/.test(text)) {
    throw new RemittanceFormatError('amount is not a decimal with up to 2 places', lineNo);
  }
  return Number(text);
}

function parseRemittance(text) {
  const rows = String(text).split(/\r?\n/);
  const result = { header: null, entries: [], trailer: null };
  let i = 0;

  while (i < rows.length) {
    const line = rows[i].trim();
    const lineNo = i + 1;
    if (line === '' || line.startsWith('#')) {
      continue;
    }
    const fields = line.split('|');
    switch (fields[0]) {
      case 'H':
        result.header = { partnerId: fields[1], batchId: fields[2], valueDate: fields[3] };
        break;
      case 'D':
        result.entries.push({
          loanId: fields[1],
          amount: parseAmount(fields[2], lineNo),
          partnerTxnRef: fields[3],
        });
        break;
      case 'T':
        result.trailer = { lineCount: Number(fields[1]), controlTotal: parseAmount(fields[2], lineNo) };
        break;
      default:
        throw new RemittanceFormatError(`unknown record type ${fields[0]}`, lineNo);
    }
    i += 1;
  }

  return validateTotals(result);
}

function validateTotals(result) {
  if (!result.header || !result.trailer) {
    throw new RemittanceFormatError('missing header or trailer record');
  }
  if (result.trailer.lineCount !== result.entries.length) {
    throw new RemittanceFormatError('trailer line count does not match detail records');
  }
  const sum = result.entries.reduce((acc, e) => acc + Math.round(e.amount * 100), 0);
  if (sum !== Math.round(result.trailer.controlTotal * 100)) {
    throw new RemittanceFormatError('control total does not match detail records');
  }
  return result;
}

module.exports = { parseRemittance, RemittanceFormatError };
