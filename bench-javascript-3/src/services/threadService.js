'use strict';

const Boom = require('@hapi/boom');
const { Thread, Post } = require('../models');
const { excerpt } = require('../lib/render');

const SEARCH_LIMIT = 50;

function toSummary(thread) {
  return {
    id: thread._id.toString(),
    title: thread.title,
    tags: thread.tags,
    locked: thread.locked,
    postCount: thread.postCount,
    lastPostAt: thread.lastPostAt,
  };
}

async function createThread(authorId, { title, tags, body }) {
  const session = await Thread.startSession();
  try {
    let created;
    await session.withTransaction(async () => {
      const [thread] = await Thread.create(
        [{ title, tags, author: authorId, postCount: 1, lastPostAt: new Date() }],
        { session }
      );
      await Post.create([{ thread: thread._id, author: authorId, body }], { session });
      created = thread;
    });
    return toSummary(created);
  } finally {
    await session.endSession();
  }
}

async function getThreadWithPosts(threadId, { page, pageSize }) {
  const thread = await Thread.findById(threadId).lean();
  if (!thread) {
    throw Boom.notFound('Thread not found');
  }
  const posts = await Post.find({ thread: threadId, deleted: false })
    .sort({ createdAt: 1 })
    .skip((page - 1) * pageSize)
    .limit(pageSize)
    .lean();
  return { thread: toSummary(thread), posts };
}

async function listThreads({ page, pageSize }) {
  const [items, total] = await Promise.all([
    Thread.find({})
      .sort({ lastPostAt: -1 })
      .skip((page - 1) * pageSize)
      .limit(pageSize)
      .lean(),
    Thread.estimatedDocumentCount(),
  ]);
  return {
    items: items.map(toSummary),
    page,
    pageSize,
    total,
  };
}

async function firstPostExcerpts(threadIds) {
  const posts = await Post.find({ thread: { $in: threadIds } }).sort({ createdAt: 1 }).lean();
  const byThread = new Map();
  for (const post of posts) {
    const key = post.thread.toString();
    if (!byThread.has(key)) byThread.set(key, excerpt(post.body));
  }
  return byThread;
}

async function searchThreads(q) {
  const needle = q.trim().toLowerCase();
  const threads = await Thread.find({
    $where: `this.title.toLowerCase().indexOf('${needle}') !== -1`,
  })
    .sort({ lastPostAt: -1 })
    .limit(SEARCH_LIMIT)
    .lean();
  const excerpts = await firstPostExcerpts(threads.map((t) => t._id));
  return threads.map((t) => ({ ...toSummary(t), excerpt: excerpts.get(t._id.toString()) || '' }));
}

const TAG_PAGE_SIZE = 30;

function normalizeTag(tag) {
  return String(tag).toLowerCase();
}

async function threadsByTag(tag) {
  const wanted = normalizeTag(tag);
  const threads = await Thread.find({
    $where: `this.tags.indexOf('${wanted}') !== -1`,
  })
    .sort({ lastPostAt: -1 })
    .limit(TAG_PAGE_SIZE)
    .lean();
  return threads.map(toSummary);
}

module.exports = {
  createThread,
  getThreadWithPosts,
  listThreads,
  searchThreads,
  threadsByTag,
};
