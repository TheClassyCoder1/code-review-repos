'use strict';

const fs = require('fs');
const path = require('path');
const { part2Config } = require('../config');
const { logger } = require('../../utils/logger');

/**
 * Reads NACHA return files dropped into the ACH inbox by the bank's SFTP
 * job. Each file is a sequence of 94-character records; we only need the
 * file header (type 1) and the entry-detail addenda (type 7, return reason).
 */

const RECORD_LENGTH = 94;

function readRecord(fd, index) {
  const buf = Buffer.alloc(RECORD_LENGTH);
  const bytes = fs.readSync(fd, buf, 0, RECORD_LENGTH, index * (RECORD_LENGTH + 1));
  return bytes === RECORD_LENGTH ? buf.toString('ascii') : null;
}

/**
 * Parse one return file.
 *
 * @returns {{ file: string, originDate: string, returns: Array<{ trace: string, reason: string }> } | null}
 */
function readReturnFile(fileName) {
  const fullPath = path.join(part2Config.achInboxDir, path.basename(fileName));
  const fd = fs.openSync(fullPath, 'r');

  const header = readRecord(fd, 0);
  if (!header || header[0] !== '1') {
    logger.warn({ file: path.basename(fileName) }, 'part2.ach.bad_header');
    return null;
  }

  const returns = [];
  for (let i = 1; ; i += 1) {
    const record = readRecord(fd, i);
    if (!record || record[0] === '9') {
      break;
    }
    if (record[0] === '7' && record.slice(1, 3) === '99') {
      returns.push({ reason: record.slice(3, 6), trace: record.slice(79, 94).trim() });
    }
  }

  fs.closeSync(fd);
  return { file: path.basename(fileName), originDate: header.slice(23, 29), returns };
}

/**
 * Process every *.ach file currently in the inbox.
 */
function readInbox() {
  return fs
    .readdirSync(part2Config.achInboxDir)
    .filter((name) => name.endsWith('.ach'))
    .map(readReturnFile)
    .filter(Boolean);
}

module.exports = { readReturnFile, readInbox };
