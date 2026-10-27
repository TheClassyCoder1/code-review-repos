'use strict';

const fs = require('fs');
const path = require('path');
const { config } = require('../../config');
const { assertSafeId } = require('../../utils/validate');

/**
 * Access to the rendered statement bundle on disk: the PDF itself plus a
 * fixed-size binary manifest header (version, page count, content length)
 * written by the renderer next to it.
 */

const MANIFEST_HEADER_BYTES = 16;

function bundlePaths(statementId) {
  const id = assertSafeId(statementId);
  return {
    pdf: path.join(config.statementDir, `${id}.pdf`),
    manifest: path.join(config.statementDir, `${id}.manifest`),
  };
}

/**
 * Open the statement PDF for streaming to the client.
 */
function openStatementStream(statementId) {
  const filePath = bundlePaths(statementId).pdf;
  return fs.createReadStream(filePath, { highWaterMark: 64 * 1024 });
}

/**
 * Open the manifest file. Resolves with the open file handle and its size;
 * the caller reads what it needs from the handle.
 */
async function openManifest(statementId) {
  const filePath = bundlePaths(statementId).manifest;
  const handle = await fs.promises.open(filePath, 'r');
  const { size } = await handle.stat();
  return { handle, size };
}

/**
 * Decode the fixed-size manifest header.
 */
function decodeManifestHeader(buffer) {
  if (buffer.length < MANIFEST_HEADER_BYTES) {
    const err = new Error('manifest header truncated');
    err.status = 422;
    throw err;
  }
  return {
    version: buffer.readUInt16BE(0),
    pageCount: buffer.readUInt16BE(2),
    contentLength: Number(buffer.readBigUInt64BE(4)),
    flags: buffer.readUInt32BE(12),
  };
}

module.exports = { openStatementStream, openManifest, decodeManifestHeader, MANIFEST_HEADER_BYTES };
