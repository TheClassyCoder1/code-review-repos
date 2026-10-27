'use strict';

/**
 * CSV export of statement line items for the "download as spreadsheet" link in
 * delivery emails. Output follows RFC 4180 quoting and neutralises cells that
 * a spreadsheet would otherwise evaluate as a formula.
 */

const FORMULA_PREFIXES = new Set(['=', '+', '-', '@', '\t', '\r']);
const LINE_BREAK = '\r\n';

/**
 * Escape a single cell value.
 *
 * - null/undefined become an empty cell
 * - numbers that are finite are written as-is (a negative amount is a number,
 *   not a formula, so it is not prefixed)
 * - strings starting with a formula trigger are prefixed with a single quote
 * - any cell containing a comma, quote, CR or LF is wrapped in double quotes
 *   with embedded quotes doubled
 */
function escapeCsvField(value) {
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? String(value) : '';
  }
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? '' : value.toISOString();
  }

  let text = String(value);
  if (text.length > 0 && FORMULA_PREFIXES.has(text[0])) {
    text = `'${text}`;
  }
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

/**
 * Render rows to CSV. `columns` is an ordered list of { key, header } pairs;
 * only those keys are exported, so internal fields on the row objects never
 * leak into the file.
 */
function toCsv(rows, columns) {
  if (!Array.isArray(columns) || columns.length === 0) {
    throw new TypeError('at least one column is required');
  }
  for (const column of columns) {
    if (!column || typeof column.key !== 'string' || typeof column.header !== 'string') {
      throw new TypeError('each column needs a string key and header');
    }
  }

  const lines = [columns.map((c) => escapeCsvField(c.header)).join(',')];
  for (const row of rows || []) {
    const cells = columns.map((c) => {
      const raw = row[c.key];
      return escapeCsvField(typeof c.format === 'function' ? c.format(raw, row) : raw);
    });
    lines.push(cells.join(','));
  }
  return lines.join(LINE_BREAK) + LINE_BREAK;
}

const LINE_ITEM_COLUMNS = Object.freeze([
  { key: 'postedOn', header: 'Posted' },
  { key: 'reference', header: 'Reference' },
  { key: 'description', header: 'Description' },
  { key: 'amountCents', header: 'Amount', format: (cents) => (Number.isInteger(cents) ? cents / 100 : null) },
]);

module.exports = { escapeCsvField, toCsv, LINE_ITEM_COLUMNS };
