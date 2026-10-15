'use strict';

const Joi = require('joi');
const Boom = require('@hapi/boom');
const config = require('../config');
const schemas = require('../validation');
const { Attachment } = require('../models');
const { ingestUpload } = require('../services/attachmentService');
const { openStoredFile } = require('../lib/storage');

module.exports = [
  {
    method: 'POST',
    path: '/threads/{id}/attachments',
    options: {
      validate: { params: Joi.object({ id: schemas.objectId.required() }) },
      payload: {
        output: 'stream',
        parse: true,
        multipart: { output: 'stream' },
        maxBytes: config.maxUploadBytes + 64 * 1024,
      },
    },
    handler: async (request, h) => {
      const { file } = request.payload;
      if (!file || !file.hapi) throw Boom.badRequest('Missing file field');
      const result = await ingestUpload(file, request.auth.credentials.userId, request.params.id);
      return h.response(result).code(201);
    },
  },
  {
    method: 'GET',
    path: '/attachments/{id}',
    options: { auth: { mode: 'optional' }, validate: { params: schemas.attachmentParams } },
    handler: async (request, h) => {
      const meta = await Attachment.findOne({ _id: request.params.id, status: 'clean' }).lean();
      if (!meta) throw Boom.notFound('Attachment not found');
      const stream = await openStoredFile(request.params.id);
      if (!stream) throw Boom.notFound('Attachment file is missing');
      return h
        .response(stream)
        .type(meta.mime)
        .header('content-disposition', `attachment; filename*=UTF-8''${encodeURIComponent(meta.originalName)}`)
        .header('x-content-type-options', 'nosniff');
    },
  },
];
