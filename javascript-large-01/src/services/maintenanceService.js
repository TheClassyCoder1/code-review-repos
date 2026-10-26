'use strict';

const { exec } = require('child_process');
const path = require('path');
const { config } = require('../config');
const { assertSafeId, assertFormat } = require('../utils/validate');

/**
 * Operator maintenance tasks that shell out to archival tooling. Every value
 * that reaches a command line is run through a strict validator first, so the
 * interpolated tokens can only ever be a bounded character set.
 */

/**
 * Compress a rendered statement bundle for cold storage. The bundle id is
 * validated to the safe-id character class before it is used.
 */
function compressBundle(bundleId) {
  const safeId = assertSafeId(bundleId);
  const bundlePath = path.join(config.statementDir, 'bundles', `${safeId}.tar`);
  return new Promise((resolve, reject) => {
    exec(`gzip -f -9 "${bundlePath}"`, { timeout: 20000 }, (err) =>
      err ? reject(err) : resolve(`${bundlePath}.gz`)
    );
  });
}

/**
 * Export a daily statement roll-up to the requested format. The format token is
 * restricted to a fixed allow-list before it is handed to the exporter CLI.
 */
function exportRollup(day, format) {
  const safeDay = assertSafeId(day);
  const safeFormat = assertFormat(format);
  const outPath = path.join(config.statementDir, 'rollups', `${safeDay}.${safeFormat}`);
  return new Promise((resolve, reject) => {
    exec(
      `stmt-rollup --day ${safeDay} --format ${safeFormat} --out "${outPath}"`,
      { timeout: 25000 },
      (err) => (err ? reject(err) : resolve(outPath))
    );
  });
}

module.exports = { compressBundle, exportRollup };
