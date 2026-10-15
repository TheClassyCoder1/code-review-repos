'use strict';

const Joi = require('joi');
const schemas = require('../validation');
const posts = require('../services/postService');
const drafts = require('../services/draftService');

const idParams = Joi.object({ id: schemas.objectId.required() });

module.exports = [
  {
    method: 'POST',
    path: '/threads/{id}/posts',
    options: { validate: { params: idParams, payload: schemas.postBody } },
    handler: async (request, h) => {
      const post = await posts.createPost(request.params.id, request.auth.credentials.userId, request.payload.body);
      return h.response({ id: post._id.toString(), renderedBody: post.renderedBody }).code(201).unstate(drafts.DRAFT_COOKIE);
    },
  },
  {
    method: 'PUT',
    path: '/posts/{id}',
    options: { validate: { params: idParams, payload: schemas.postBody } },
    handler: async (request) => {
      const post = await posts.editPost(request.params.id, request.auth.credentials.userId, request.payload.body);
      return { id: post._id.toString(), renderedBody: post.renderedBody, editedAt: post.editedAt };
    },
  },
  {
    method: 'DELETE',
    path: '/posts/{id}',
    options: { validate: { params: idParams } },
    handler: (request) => posts.deletePost(request.params.id, request.auth.credentials),
  },
  {
    method: 'GET',
    path: '/posts/{id}/embed',
    options: { auth: false, validate: { params: idParams } },
    handler: async (request, h) => h.response(await posts.renderPostEmbed(request.params.id)).type('text/html'),
  },
  {
    method: 'GET',
    path: '/drafts/current',
    handler: (request) => drafts.loadDraftFromCookie(request) || {},
  },
  {
    method: 'PUT',
    path: '/drafts/current',
    options: { validate: { payload: Joi.object({ thread: schemas.objectId, body: Joi.string().max(20000).allow('') }) } },
    handler: (request, h) => h.response({ saved: true }).state(drafts.DRAFT_COOKIE, drafts.cookieValueFor(request.payload)),
  },
  {
    method: 'GET',
    path: '/drafts/{key}',
    options: { validate: { params: Joi.object({ key: Joi.string().alphanum().max(32).required() }) } },
    handler: (request) => drafts.getDraft(request.params.key, request.auth.credentials.userId),
  },
  {
    method: 'POST',
    path: '/drafts/import',
    options: { validate: { payload: schemas.draftImport } },
    handler: (request) => drafts.restoreDraft(request.auth.credentials.userId, request.payload.blob),
  },
];
