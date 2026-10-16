'use strict';

const Joi = require('joi');

const objectId = Joi.string().hex().length(24);

const register = Joi.object({
  email: Joi.string().email().max(254).required(),
  password: Joi.string().min(10).max(128).required(),
  displayName: Joi.string().min(2).max(40).required(),
});

const login = Joi.object({
  email: Joi.string().email().max(254).required(),
  password: Joi.string().max(128).required(),
});

const logoutQuery = Joi.object({
  next: Joi.string().pattern(/^\/(?!\/)[\w\-/]*$/).max(200),
});

const ssoCallbackQuery = Joi.object({
  code: Joi.string().max(512).required(),
  state: Joi.string().max(1024).required(),
});

const threadSearchQuery = Joi.object({
  q: Joi.string().min(2).max(100).required(),
});

const tagParams = Joi.object({
  tag: Joi.string().pattern(/^[a-z0-9-]{1,32}$/).required(),
});

const listQuery = Joi.object({
  page: Joi.number().integer().min(1).max(500).default(1),
  pageSize: Joi.number().integer().min(1).max(50).default(20),
});

const createThread = Joi.object({
  title: Joi.string().min(3).max(140).required(),
  tags: Joi.array().items(Joi.string().pattern(/^[a-z0-9-]{1,32}$/)).max(5).default([]),
  body: Joi.string().min(1).max(20000).required(),
});

const postBody = Joi.object({
  body: Joi.string().min(1).max(20000).required(),
});

const attachmentParams = Joi.object({
  id: objectId.required(),
});

const lockThread = Joi.object({
  reason: Joi.string().min(3).max(200).required(),
});

const resolveReport = Joi.object({
  outcome: Joi.string().valid('dismissed', 'actioned').required(),
});

const draftImport = Joi.object({
  blob: Joi.string().base64().max(64 * 1024).required(),
});

module.exports = {
  objectId,
  register,
  login,
  logoutQuery,
  ssoCallbackQuery,
  threadSearchQuery,
  tagParams,
  listQuery,
  createThread,
  postBody,
  attachmentParams,
  lockThread,
  resolveReport,
  draftImport,
};
