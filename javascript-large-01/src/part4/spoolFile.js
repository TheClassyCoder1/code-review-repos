'use strict';

const fs = require('fs');
const path = require('path');
const { assertSafeId } = require('../utils/validate');

/**
 * Low-level helpers for the collections export spool. An export is written as
 * a CSV file in the spool directory and later picked up by the uploader.
 *
 * These helpers deliberately do not manage handle lifetime: opening, writing
 * and closing are composed by spoolScope.withSpoolFile, which owns the handle
 * for the whole export.
 */

const CSV_COLUMNS = ['loanId', 'bucket', 'daysPastDue', 'outstanding', 'feesDue'];

/**
 * Open a new spool file for writing. Fails if a file with the same export id
 * already exists so two exports never interleave in one file.
 */
async function openSpoolFile(spoolDir, exportId) {
  const fileName = `${assertSafeId(exportId)}.csv`;
  const filePath = path.join(spoolDir, fileName);
  const handle = await fs.promises.open(filePath, 'wx', 0o640);
  return { handle, filePath };
}

/**
 * Quote a single CSV field per RFC 4180. Fields that start with a spreadsheet
 * formula trigger character are prefixed with a single quote so an operator
 * opening the export in a spreadsheet does not execute them.
 */
function csvField(value) {
  if (value === null || value === undefined) {
    return '';
  }
  let text = String(value);
  if (/^[=+\-@\t\r]/.test(text) && !/^-?\d+(\.\d+)?$/.test(text)) {
    text = `'${text}`;
  }
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

/**
 * Render one aged-loan record as a CSV line in CSV_COLUMNS order.
 */
function formatCsvRow(record) {
  if (!record || typeof record !== 'object') {
    throw new TypeError('record must be an object');
  }
  const cells = CSV_COLUMNS.map((column) => {
    const value = record[column];
    if (typeof value === 'number') {
      if (!Number.isFinite(value)) {
        throw new RangeError(`column ${column} is not a finite number`);
      }
      return column === 'daysPastDue' ? String(value) : value.toFixed(2);
    }
    return csvField(value);
  });
  return `${cells.join(',')}\r\n`;
}

async function writeHeader(handle) {
  await handle.write(`${CSV_COLUMNS.join(',')}\r\n`);
}

async function writeRows(handle, records) {
  let written = 0;
  for (const record of records) {
    await handle.write(formatCsvRow(record));
    written += 1;
  }
  return written;
}

module.exports = { openSpoolFile, formatCsvRow, csvField, writeHeader, writeRows, CSV_COLUMNS };
