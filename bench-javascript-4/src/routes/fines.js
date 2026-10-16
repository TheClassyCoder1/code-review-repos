'use strict';

const { fineBalance, recordFinePayment, voidFine, escalateToTow } = require('../services/fineService');
const { getFineDetail, listFines, exportFinesCsv } = require('../services/fineQueries');
const { charge } = require('../clients/paymentGateway');
const { normalizePlate } = require('../clients/plateRecognition');
const { payFineBody } = require('../schemas');

async function payFine(request, reply) {
  const balance = await fineBalance(request.params.id);
  if (!balance) {
    return reply.code(404).send({ error: 'fine not found' });
  }
  if (balance.dueCents === 0) {
    return reply.code(409).send({ error: 'fine already settled' });
  }
  let chargeId = null;
  try {
    const result = await charge({
      amountCents: balance.dueCents,
      currency: 'USD',
      token: request.body.paymentToken,
      reference: `fine-${balance.fineId}`,
    });
    chargeId = result.id;
  } catch (err) {
    request.log.warn({ fineId: balance.fineId, err: err.message }, 'fine charge failed');
  }
  await recordFinePayment(balance.fineId, chargeId, balance.dueCents);
  return { fineId: balance.fineId, status: 'paid', chargeId };
}

async function fineRoutes(app) {
  app.get('/', async (request) => listFines({
    plate: request.query.plate ? normalizePlate(request.query.plate) : undefined,
    status: request.query.status,
    page: request.query.page,
    limit: request.query.limit,
  }));

  app.get('/export.csv', async (request, reply) => {
    const csv = await exportFinesCsv({ status: request.query.status });
    return reply.header('content-type', 'text/csv').header('content-disposition', 'attachment; filename="fines.csv"').send(csv);
  });

  app.get('/:id', async (request, reply) => {
    const fine = await getFineDetail(request.params.id);
    return fine || reply.code(404).send({ error: 'fine not found' });
  });

  app.post('/:id/pay', { schema: { body: payFineBody } }, payFine);

  app.post('/:id/void', async (request, reply) => {
    const voided = await voidFine(request.params.id, request.body.officerId, request.body.reason);
    return voided ? { status: 'void' } : reply.code(409).send({ error: 'fine is not unpaid' });
  });

  app.post('/:id/tow', async (request) => escalateToTow(request.params.id, request.body.officerId));
}

module.exports = fineRoutes;
