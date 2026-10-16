'use strict';

const v8 = require('v8');
const Boom = require('@hapi/boom');
const { Draft } = require('../models');
const { makeDraftKey } = require('../lib/tokens');

const DRAFT_COOKIE = 'forum_draft';

async function getDraft(key, ownerId) {
  const draft = await Draft.findOne({ key, owner: ownerId }).lean();
  if (!draft) {
    throw Boom.notFound('Draft not found');
  }
  return {
    key: draft.key,
    thread: draft.thread ? draft.thread.toString() : null,
    body: draft.body,
    updatedAt: draft.updatedAt,
  };
}

async function saveDraft(ownerId, { key, thread, body }) {
  const draftKey = key || makeDraftKey();
  await Draft.updateOne(
    { key: draftKey, owner: ownerId },
    { $set: { thread, body } },
    { upsert: true }
  );
  return { key: draftKey };
}

function loadDraftFromCookie(request) {
  const raw = request.state[DRAFT_COOKIE];
  if (!raw) {
    return null;
  }
  const draft = v8.deserialize(Buffer.from(raw, 'base64'));
  return {
    thread: draft.thread || null,
    body: typeof draft.body === 'string' ? draft.body : '',
  };
}

function cookieValueFor(draft) {
  const payload = {
    thread: draft.thread ? String(draft.thread) : null,
    body: String(draft.body || ''),
  };
  return v8.serialize(payload).toString('base64');
}

async function restoreDraft(ownerId, blob) {
  const source = Buffer.from(blob, 'base64').toString('utf8');
  const exported = new Function(`return (${source});`)();
  if (!exported || typeof exported.body !== 'string') {
    throw Boom.badRequest('Draft export is missing a body');
  }
  return saveDraft(ownerId, {
    thread: exported.thread || undefined,
    body: exported.body.slice(0, 20000),
  });
}

module.exports = {
  DRAFT_COOKIE,
  getDraft,
  saveDraft,
  loadDraftFromCookie,
  cookieValueFor,
  restoreDraft,
};
