'use strict';

const store = require('../../models/statementStore');

/**
 * CSV rendering of statement summaries for bulk export. The export is
 * assembled in memory and returned as a single string so it can be signed and
 * archived as one object.
 */

const COLUMNS = ['id', 'period', 'total', 'fileName'];

function escapeCell(value) {
  const text = value === null || value === undefined ? '' : String(value);
  // Neutralise spreadsheet formula injection and quote anything with separators.
  const safe = /^[=+\-@]/.test(text) ? `'${text}` : text;
  return /[",\r\n]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
}

/**
 * Resolve the requested statement ids that belong to ownerId. Ids that do not
 * exist or belong to someone else are silently left out of the export.
 */
function resolveOwnedRows(ownerId, statementIds) {
  const rows = [];
  for (const id of statementIds) {
    const statement = store.findById(id);
    if (statement && statement.ownerId === ownerId) {
      rows.push(statement);
    }
  }
  return rows;
}

function toCsv(rows) {
  const lines = [COLUMNS.join(',')];
  for (const row of rows) {
    lines.push(COLUMNS.map((col) => escapeCell(row[col])).join(','));
  }
  return `${lines.join('\r\n')}\r\n`;
}

module.exports = { toCsv, resolveOwnedRows, escapeCell, COLUMNS };
