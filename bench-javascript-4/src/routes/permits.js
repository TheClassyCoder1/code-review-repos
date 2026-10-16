'use strict';

const { listPermits, issuePermit, cancelPermit } = require('../services/permitService');
const { renewWithPayment } = require('../services/renewalService');
const { importPermitExpiries } = require('../import/permitImport');
const { refund } = require('../clients/paymentGateway');
const { listPermitsQuery, createPermitBody, renewPermitBody } = require('../schemas');

const importBody = {
  type: 'object',
  required: ['file'],
  properties: { file: { type: 'string', pattern: '^[A-Za-z0-9_.-]{1,80}\\.csv$' } },
};

async function permitRoutes(app) {
  app.get('/', { schema: { querystring: listPermitsQuery } }, async (request) => listPermits(request.query));

  app.post('/', { schema: { body: createPermitBody } }, async (request, reply) => {
    const permit = await issuePermit(request.body);
    return reply.code(201).send(permit);
  });

  app.post('/:id/renew', { schema: { body: renewPermitBody } }, async (request) =>
    renewWithPayment(request.params.id, request.body.months, request.body.paymentToken));

  app.post('/:id/cancel', async (request) => {
    const { permit, refund: owed } = await cancelPermit(request.params.id);
    if (permit.charge_id && owed.refundCents > 0) {
      await refund(permit.charge_id, owed.refundCents);
    }
    return { id: permit.id, status: 'cancelled', refund_cents: owed.refundCents };
  });

  app.post('/import', { schema: { body: importBody } }, async (request) => importPermitExpiries(request.body.file));
}

module.exports = permitRoutes;
