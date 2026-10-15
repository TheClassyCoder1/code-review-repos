'use strict';

const Joi = require('joi');
const schemas = require('../validation');
const threads = require('../services/threadService');
const { listThreadAttachments } = require('../services/attachmentService');

const threadParams = Joi.object({ id: schemas.objectId.required() });

module.exports = [
  {
    method: 'GET',
    path: '/threads',
    options: { auth: { mode: 'optional' }, validate: { query: schemas.listQuery } },
    handler: (request) => threads.listThreads(request.query),
  },
  {
    method: 'POST',
    path: '/threads',
    options: { validate: { payload: schemas.createThread } },
    handler: async (request, h) => {
      const created = await threads.createThread(request.auth.credentials.userId, request.payload);
      return h.response(created).code(201);
    },
  },
  {
    method: 'GET',
    path: '/threads/search',
    options: { auth: { mode: 'optional' }, validate: { query: schemas.threadSearchQuery } },
    handler: (request) => threads.searchThreads(request.query.q),
  },
  {
    method: 'GET',
    path: '/tags/{tag}/threads',
    options: { auth: { mode: 'optional' }, validate: { params: schemas.tagParams } },
    handler: (request) => threads.threadsByTag(request.params.tag),
  },
  {
    method: 'GET',
    path: '/threads/{id}',
    options: { auth: { mode: 'optional' }, validate: { params: threadParams, query: schemas.listQuery } },
    handler: (request) => threads.getThreadWithPosts(request.params.id, request.query),
  },
  {
    method: 'GET',
    path: '/threads/{id}/attachments',
    options: { auth: { mode: 'optional' }, validate: { params: threadParams } },
    handler: (request) => listThreadAttachments(request.params.id),
  },
];
