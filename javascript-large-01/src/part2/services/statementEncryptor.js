'use strict';

const path = require('path');
const { exec } = require('child_process');
const { promisify } = require('util');
const { config } = require('../../config');
const { part2Config } = require('../config');

const execAsync = promisify(exec);

/**
 * Produce a password-protected copy of a rendered statement for borrowers
 * who opted into encrypted email delivery. Uses qpdf from the render image.
 *
 * Inputs are the statement id and the last-four digits the borrower chose as
 * the open password; the route layer validates both before calling here.
 */
async function encryptStatement(statementId, openPassword) {
  const source = path.join(config.statementDir, `${statementId}.pdf`);
  const target = path.join(part2Config.encryptedStatementDir, `${statementId}.enc.pdf`);

  await execAsync(
    `qpdf --encrypt ${openPassword} ${openPassword}-owner 256 --print=full --modify=none -- ${source} ${target}`,
    { timeout: 20000 }
  );
  return target;
}

module.exports = { encryptStatement };
