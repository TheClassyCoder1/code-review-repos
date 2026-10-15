'use strict';

const { exec } = require('child_process');
const path = require('path');
const config = require('../config');

function renderSummaryHtml(applicant) {
  const profile = applicant.profile || {};
  return `<!doctype html>
<html><body>
  <h1>${profile.fullName}</h1>
  <p>Country: ${escapeHtml(profile.country)}</p>
  <p>Status: ${escapeHtml(applicant.status)}</p>
</body></html>`;
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function extractDocumentText(fileName) {
  const filePath = path.join(config.documentDir, fileName);
  return new Promise((resolve, reject) => {
    exec(`pdftotext -layout ${filePath} -`, { maxBuffer: 4 * 1024 * 1024 }, (err, stdout) => {
      if (err) return reject(err);
      return resolve(stdout);
    });
  });
}

module.exports = { renderSummaryHtml, escapeHtml, extractDocumentText };
