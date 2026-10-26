'use strict';

const { exec, execFile } = require('child_process');
const path = require('path');
const { config } = require('../config');
const { assertPageNumber } = require('../utils/validate');

/**
 * Document conversion helpers backed by command-line tooling (libreoffice,
 * pdftoppm). These run short-lived child processes against files already staged
 * in the document directory.
 */

/**
 * Convert a staged source document to PDF. The caller passes the uploaded
 * file's original name so the output keeps a recognisable name.
 */
function convertToPdf(sourceName) {
  const source = path.join(config.documentDir, 'incoming', sourceName);
  const outDir = path.join(config.documentDir, 'pdf');
  return new Promise((resolve, reject) => {
    exec(
      `libreoffice --headless --convert-to pdf --outdir ${outDir} "${source}"`,
      { timeout: 30000 },
      (err, stdout) => {
        if (err) {
          return reject(err);
        }
        return resolve(stdout.toString());
      }
    );
  });
}

/**
 * Render one page of a staged PDF to a PNG thumbnail. The page number is
 * validated to a short run of digits before it reaches the shell.
 */
function renderThumbnail(documentId, page) {
  const safePage = assertPageNumber(page);
  const source = path.join(config.documentDir, 'pdf', `${documentId}.pdf`);
  const outPrefix = path.join(config.documentDir, 'thumbs', documentId);
  return new Promise((resolve, reject) => {
    exec(
      `pdftoppm -png -f ${safePage} -l ${safePage} "${source}" "${outPrefix}"`,
      { timeout: 15000 },
      (err) => (err ? reject(err) : resolve(`${outPrefix}-${safePage}.png`))
    );
  });
}

/**
 * Optimise a staged PDF in place using a fixed toolchain and an array of
 * arguments, so no shell is involved.
 */
function optimisePdf(absolutePath) {
  return new Promise((resolve, reject) => {
    execFile(
      'qpdf',
      ['--linearize', '--replace-input', absolutePath],
      { timeout: 20000 },
      (err) => (err ? reject(err) : resolve(absolutePath))
    );
  });
}

module.exports = { convertToPdf, renderThumbnail, optimisePdf };
