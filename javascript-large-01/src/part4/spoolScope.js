'use strict';

const fs = require('fs');
const { openSpoolFile, writeHeader } = require('./spoolFile');
const { logger } = require('../utils/logger');

/**
 * Owns the lifetime of a spool file handle. The handle opened by
 * openSpoolFile is closed in a finally block whatever the body does, and a
 * partially written file is removed when the body throws so the uploader never
 * sees a truncated export.
 *
 * This is the only module that calls openSpoolFile.
 */
async function withSpoolFile(spoolDir, exportId, body) {
  const { handle, filePath } = await openSpoolFile(spoolDir, exportId);
  let succeeded = false;
  try {
    await writeHeader(handle);
    const result = await body(handle);
    await handle.sync();
    succeeded = true;
    return { result, filePath };
  } finally {
    await handle.close();
    if (!succeeded) {
      await fs.promises.unlink(filePath).catch((err) => {
        logger.warn({ exportId, code: err.code }, 'spool.cleanup.failed');
      });
    }
  }
}

module.exports = { withSpoolFile };
