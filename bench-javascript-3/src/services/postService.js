'use strict';

const Boom = require('@hapi/boom');
const { Post, Thread, User } = require('../models');
const { escapeHtml, renderSignatureHtml } = require('../lib/render');
const { firstLink, buildLinkPreview } = require('./linkPreview');
const { checkSpam } = require('./moderationService');

function isModerator(credentials) {
  return Array.isArray(credentials.scope) && credentials.scope.some((s) => s === 'moderator' || s === 'admin');
}

async function createPost(threadId, authorId, body) {
  const thread = await Thread.findById(threadId);
  if (!thread) throw Boom.notFound('Thread not found');
  if (thread.locked) throw Boom.forbidden('Thread is locked');
  const post = new Post({ thread: threadId, author: authorId, body });
  if (await checkSpam(post)) throw Boom.badRequest('Post was flagged as spam');
  const link = firstLink(body);
  if (link) {
    post.linkPreview = await buildLinkPreview(link).catch(() => null);
  }
  await post.save();
  await Thread.updateOne({ _id: threadId }, { $inc: { postCount: 1 }, $set: { lastPostAt: new Date() } });
  return post;
}

async function editPost(postId, userId, body) {
  const post = await Post.findById(postId);
  if (!post || post.deleted) {
    throw Boom.notFound('Post not found');
  }
  const thread = await Thread.findById(post.thread).lean();
  if (thread && thread.locked) {
    throw Boom.forbidden('Thread is locked');
  }
  post.body = body;
  post.editedAt = new Date();
  await post.save();
  return post;
}

async function loadAuthor(userId) {
  const user = await User.findById(userId).lean();
  return user || { displayName: 'deleted user', signature: '' };
}

async function deletePost(postId, credentials) {
  const post = await Post.findById(postId);
  if (!post || post.deleted) {
    throw Boom.notFound('Post not found');
  }
  if (!post.author.equals(credentials.userId) && !isModerator(credentials)) {
    throw Boom.forbidden('You can only delete your own posts');
  }
  post.deleted = true;
  await post.save();
  await Thread.updateOne({ _id: post.thread }, { $inc: { postCount: -1 } });
  return { id: post._id.toString(), deleted: true };
}

const EMBED_STYLES = 'font-family:system-ui;max-width:640px;border:1px solid #ddd;padding:12px';

function embedFooter(threadId) {
  return `<footer><a href="/threads/${escapeHtml(threadId)}" target="_top">View thread</a></footer>`;
}

async function renderPostEmbed(postId) {
  const post = await Post.findOne({ _id: postId, deleted: false }).lean();
  if (!post) {
    throw Boom.notFound('Post not found');
  }
  const author = await loadAuthor(post.author);
  return [
    `<article style="${EMBED_STYLES}">`,
    `<header>${escapeHtml(author.displayName)}</header>`,
    `<section>${post.renderedBody}</section>`,
    renderSignatureHtml(author),
    embedFooter(post.thread.toString()),
    '</article>',
  ].join('\n');
}

module.exports = {
  createPost,
  editPost,
  deletePost,
  renderPostEmbed,
};
