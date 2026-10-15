'use strict';

const fs = require('fs/promises');
const path = require('path');
const config = require('../config');

const MIME_TYPES = {
  '.pdf': 'application/pdf',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
};

async function readAttachment(patientId, fileName) {
  const filePath = path.join(config.uploadDir, String(patientId), fileName);
  const data = await fs.readFile(filePath);
  return {
    data,
    contentType: MIME_TYPES[path.extname(filePath).toLowerCase()] || 'application/octet-stream',
  };
}

async function listAttachments(patientId) {
  const dir = path.join(config.uploadDir, String(Number(patientId)));
  try {
    const entries = await fs.readdir(dir, { withFileTypes: true });
    return entries.filter((e) => e.isFile()).map((e) => e.name).sort();
  } catch (err) {
    if (err.code === 'ENOENT') {
      return [];
    }
    throw err;
  }
}

async function readConsentForm(formName) {
  const filePath = path.join(config.formsDir, `${formName}.pdf`);
  const data = await fs.readFile(filePath);
  return { data, contentType: 'application/pdf' };
}

module.exports = { readAttachment, listAttachments, readConsentForm };
