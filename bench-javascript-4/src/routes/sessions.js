'use strict';

const { startSession, endSession, listActiveSessions } = require('../services/sessionService');
const { adjustOccupancy } = require('../services/lotService');
const { recognizePlate } = require('../clients/plateRecognition');

const MAX_IMAGE_BYTES = 2 * 1024 * 1024;

const entryQuery = {
  type: 'object',
  required: ['lotId', 'cameraId'],
  properties: {
    lotId: { type: 'integer', minimum: 1 },
    cameraId: { type: 'string', pattern: '^[a-z0-9-]{3,40}$' },
  },
};

async function sessionRoutes(app) {
  app.addContentTypeParser('application/octet-stream', { parseAs: 'buffer', bodyLimit: MAX_IMAGE_BYTES }, (_req, body, done) => {
    done(null, body);
  });

  app.get('/active', async (request) => listActiveSessions({ lotId: request.query.lotId }));

  app.post('/entry', { schema: { querystring: entryQuery } }, async (request, reply) => {
    const { lotId, cameraId } = request.query;
    const { plate } = await recognizePlate(request.body, cameraId);
    if (!plate) {
      return reply.code(422).send({ error: 'plate could not be read' });
    }
    const session = await startSession({ lotId, plate, source: 'anpr' });
    await adjustOccupancy(lotId, 1);
    return reply.code(201).send(session);
  });

  app.post('/:id/end', async (request, reply) => {
    const session = await endSession(request.params.id);
    if (!session) {
      return reply.code(404).send({ error: 'active session not found' });
    }
    return session;
  });
}

module.exports = sessionRoutes;
