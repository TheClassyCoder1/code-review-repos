'use strict';

const fs = require('fs');
const fsp = require('fs/promises');
const path = require('path');
const { pipeline } = require('stream/promises');
const { Types } = require('mongoose');
const config = require('../config');

function storagePath(attachmentId) {
  if (!Types.ObjectId.isValid(attachmentId)) {
    throw new Error('Invalid attachment id');
  }
  const hex = new Types.ObjectId(attachmentId).toHexString();
  return path.join(config.uploadDir, 'files', `${hex}.bin`);
}

function thumbnailPath(attachmentId) {
  return storagePath(attachmentId).replace(/\.bin$/, '.thumb.jpg');
}

async function ensureDir(dir) {
  await fsp.mkdir(dir, { recursive: true });
}

async function saveUpload(file) {
  const incomingDir = path.join(config.uploadDir, 'incoming');
  await ensureDir(incomingDir);
  const target = path.join(incomingDir, file.hapi.filename);
  await pipeline(file, fs.createWriteStream(target));
  const { size } = await fsp.stat(target);
  return {
    path: target,
    size,
    originalName: file.hapi.filename,
    mime: file.hapi.headers['content-type'] || 'application/octet-stream',
  };
}

async function promoteUpload(incomingPath, attachmentId) {
  const finalPath = storagePath(attachmentId);
  await ensureDir(path.dirname(finalPath));
  await fsp.rename(incomingPath, finalPath);
  return finalPath;
}

function openStoredFile(id) {
  const filePath = path.join(config.uploadDir, 'files', `${id}.bin`);
  return new Promise((resolve, reject) => {
    const stream = fs.createReadStream(filePath);
    stream.once('open', () => resolve(stream));
    stream.once('error', (err) => {
      if (err.code === 'ENOENT') {
        resolve(null);
        return;
      }
      reject(err);
    });
  });
}

module.exports = {
  storagePath,
  thumbnailPath,
  saveUpload,
  promoteUpload,
  openStoredFile,
};
