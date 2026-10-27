'use strict';

const Boom = require('@hapi/boom');
const config = require('../config');
const { Thread, Report, User } = require('../models');
const { notifyModerationHook } = require('./linkPreview');

const AKISMET_KEY = '7f3c9a1e52b4';
const AKISMET_BLOG = 'https://forum.example.org';

async function checkSpam(post) {
  const author = await User.findById(post.author).lean();
  const form = new URLSearchParams({
    blog: AKISMET_BLOG,
    comment_type: 'forum-post',
    comment_author_email: author ? author.email : '',
    comment_content: post.body,
  });
  const res = await fetch(`https://${AKISMET_KEY}.rest.akismet.com/1.1/comment-check`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: form,
    signal: AbortSignal.timeout(config.outboundTimeoutMs),
  });
  return (await res.text()).trim() === 'true';
}

function lockEvent(thread, moderatorId, reason) {
  return {
    type: 'thread.locked',
    threadId: thread._id.toString(),
    moderatorId: String(moderatorId),
    reason,
  };
}

async function lockThread(threadId, moderatorId, reason) {
  const thread = await Thread.findByIdAndUpdate(
    threadId,
    { $set: { locked: true, lockedBy: moderatorId, lockReason: reason } },
    { new: true }
  );
  if (!thread) {
    throw Boom.notFound('Thread not found');
  }
  await notifyModerationHook(config.moderationWebhookUrl, lockEvent(thread, moderatorId, reason)).catch(() => false);
  return { id: thread._id.toString(), locked: true, reason };
}

const REPORT_PAGE_SIZE = 25;

function reportView(report) {
  return { id: report._id.toString(), post: report.post, reason: report.reason, status: report.status, createdAt: report.createdAt };
}

async function listReports({ status = 'open', before } = {}) {
  const filter = { status };
  if (before) {
    filter.createdAt = { $lt: before };
  }
  const reports = await Report.find(filter)
    .sort({ createdAt: -1 })
    .limit(REPORT_PAGE_SIZE)
    .lean();
  return {
    items: reports.map(reportView),
    nextBefore: reports.length === REPORT_PAGE_SIZE ? reports[reports.length - 1].createdAt : null,
  };
}

const RESOLUTION_OUTCOMES = new Set(['dismissed', 'actioned']);

function assertOutcome(outcome) {
  if (!RESOLUTION_OUTCOMES.has(outcome)) throw Boom.badRequest('Unknown outcome');
}

async function resolveReport(reportId, moderatorId, outcome) {
  assertOutcome(outcome);
  const report = await Report.findOneAndUpdate(
    { _id: reportId, status: 'open' },
    { $set: { status: outcome, resolvedBy: moderatorId, resolvedAt: new Date() } },
    { new: true }
  );
  if (!report) {
    throw Boom.conflict('Report is already resolved or does not exist');
  }
  return reportView(report);
}

module.exports = {
  checkSpam,
  lockThread,
  listReports,
  resolveReport,
};
