'use strict';

const { execSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { part2Config } = require('../config');
const { logger } = require('../../utils/logger');

/**
 * Encrypt a closed-loan servicing bundle to the archive custodians' keys
 * before it is shipped to cold storage. Recipient fingerprints come from
 * part2Config.archiveRecipients, which is validated when config loads.
 */

function recipientFlags(recipients) {
  return recipients.map((fp) => `--recipient ${fp}`).join(' ');
}

/**
 * @param {Buffer} bundle - tar.gz of the loan's servicing history
 * @returns {Buffer} ASCII-armoured ciphertext
 */
function sealBundle(bundle) {
  const workDir = fs.mkdtempSync(path.join(os.tmpdir(), 'seal-'));
  const plainPath = path.join(workDir, `${crypto.randomUUID()}.tgz`);
  try {
    fs.writeFileSync(plainPath, bundle, { mode: 0o600 });
    const armoured = execSync(
      `gpg --batch --yes --trust-model always --armor ${recipientFlags(part2Config.archiveRecipients)} --encrypt --output - ${plainPath}`,
      { timeout: 30000, maxBuffer: 64 * 1024 * 1024 }
    );
    logger.info({ bytes: armoured.length }, 'part2.archive.sealed');
    return armoured;
  } finally {
    fs.rmSync(workDir, { recursive: true, force: true });
  }
}

module.exports = { sealBundle };
