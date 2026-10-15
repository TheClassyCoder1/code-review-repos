'use strict';

const schemas = require('../validation');
const Joi = require('joi');
const { lockThread, listReports, resolveReport } = require('../services/moderationService');

const MODERATOR_SCOPES = ['moderator', 'admin'];

function moderatorId(request) {
  return request.auth.credentials.userId;
}

async function lockThreadHandler(request) {
  return lockThread(request.params.id, moderatorId(request), request.payload.reason);
}

async function listReportsHandler(request) {
  const before = request.query.before ? new Date(request.query.before) : undefined;
  return listReports({ status: request.query.status, before });
}

async function resolveReportHandler(request) {
  return resolveReport(request.params.id, moderatorId(request), request.payload.outcome);
}

module.exports = [
  {
    method: 'POST',
    path: '/moderation/threads/{id}/lock',
    options: {
      auth: { access: { scope: MODERATOR_SCOPES } },
      validate: { params: Joi.object({ id: schemas.objectId.required() }), payload: schemas.lockThread },
    },
    handler: lockThreadHandler,
  },
  {
    method: 'GET',
    path: '/moderation/reports',
    options: {
      auth: { access: { scope: MODERATOR_SCOPES } },
      validate: {
        query: Joi.object({
          status: Joi.string().valid('open', 'dismissed', 'actioned').default('open'),
          before: Joi.date().iso(),
        }),
      },
    },
    handler: listReportsHandler,
  },
  {
    method: 'POST',
    path: '/moderation/reports/{id}/resolve',
    options: {
      auth: { access: { scope: MODERATOR_SCOPES } },
      validate: { params: Joi.object({ id: schemas.objectId.required() }), payload: schemas.resolveReport },
    },
    handler: resolveReportHandler,
  },
];
