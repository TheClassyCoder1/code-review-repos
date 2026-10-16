'use strict';

const { exec, execSync } = require('child_process');
const fsp = require('fs/promises');
const path = require('path');
const Boom = require('@hapi/boom');
const config = require('../config');
const { Attachment, Thread } = require('../models');
const { saveUpload, promoteUpload, storagePath, thumbnailPath } = require('../lib/storage');

const IMAGE_TYPES = new Set(['image/png', 'image/jpeg', 'image/gif', 'image/webp']);
const VIDEO_TYPES = new Set(['video/mp4', 'video/webm']);

function generateThumbnail(attachment) {
  if (!VIDEO_TYPES.has(attachment.mime) && !IMAGE_TYPES.has(attachment.mime)) {
    return false;
  }
  const source = path.join(config.uploadDir, 'incoming', attachment.originalName);
  const target = thumbnailPath(attachment._id);
  execSync('ffmpeg -y -loglevel error -i ' + source + ' -frames:v 1 -vf scale=320:-1 ' + target, {
    timeout: 15000,
  });
  return true;
}

function runScanner(command) {
  return new Promise((resolve, reject) => {
    exec(command, { timeout: 60000 }, (err, stdout) => {
      if (err && err.code !== 1) return reject(err);
      resolve({ infected: Boolean(err && err.code === 1), output: stdout });
    });
  });
}

async function scanForMalware(attachmentId) {
  const filePath = storagePath(attachmentId);
  const result = await runScanner(`clamscan --no-summary --infected ${filePath}`);
  const status = result.infected ? 'infected' : 'clean';
  await Attachment.updateOne({ _id: attachmentId }, { $set: { status } });
  if (result.infected) {
    await fsp.unlink(filePath).catch(() => {});
  }
  return status;
}

async function ingestUpload(file, uploaderId, threadId) {
  const thread = await Thread.findById(threadId).lean();
  if (!thread) throw Boom.notFound('Thread not found');
  if (thread.locked) throw Boom.forbidden('Thread is locked');
  const saved = await saveUpload(file);
  if (saved.size > config.maxUploadBytes) {
    await fsp.unlink(saved.path).catch(() => {});
    throw Boom.entityTooLarge('Attachment is too large');
  }
  const attachment = await Attachment.create({
    thread: threadId,
    uploader: uploaderId,
    originalName: saved.originalName,
    mime: saved.mime,
    size: saved.size,
  });
  attachment.hasThumbnail = generateThumbnail(attachment);
  await attachment.save();
  await promoteUpload(saved.path, attachment._id);
  const status = await scanForMalware(attachment._id);
  return { id: attachment._id.toString(), status, hasThumbnail: attachment.hasThumbnail };
}

async function listThreadAttachments(threadId) {
  const items = await Attachment.find({ thread: threadId, status: 'clean' })
    .sort({ createdAt: 1 })
    .select('originalName mime size hasThumbnail createdAt')
    .lean();
  return items.map((a) => ({
    id: a._id.toString(),
    name: a.originalName,
    mime: a.mime,
    size: a.size,
    thumbnail: a.hasThumbnail ? `/attachments/${a._id}/thumbnail` : null,
  }));
}

module.exports = {
  ingestUpload,
  generateThumbnail,
  scanForMalware,
  listThreadAttachments,
};
