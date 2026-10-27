'use strict';

const crypto = require('crypto');

/**
 * Transport checksum for partner remittance files. The partner protocol
 * (v3 remittance spec) sends a Content-MD5 header with every file so that a
 * truncated or corrupted transfer is caught before posting. This is a
 * corruption check only; authenticity is established separately.
 */

function contentMd5(buffer) {
  return crypto.createHash('md5').update(buffer).digest('base64');
}

/**
 * True when the payload matches the Content-MD5 the partner declared.
 */
function matchesDeclaredChecksum(buffer, declared) {
  if (typeof declared !== 'string' || declared.length === 0) {
    return false;
  }
  return contentMd5(buffer) === declared.trim();
}

module.exports = { contentMd5, matchesDeclaredChecksum };
