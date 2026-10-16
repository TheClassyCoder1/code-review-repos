'use strict';

const { listLots, getLot, createLot, lotOccupancy } = require('../services/lotService');
const { reserveBayForPlate } = require('../services/reservationService');
const { normalizePlate } = require('../clients/plateRecognition');
const { createLotBody, reserveBayBody } = require('../schemas');

const idParams = {
  type: 'object',
  required: ['id'],
  properties: { id: { type: 'integer', minimum: 1 } },
};

async function lotRoutes(app) {
  app.get('/', async (request) => listLots({ city: request.query.city }));

  app.get('/:id', { schema: { params: idParams } }, async (request) => getLot(request.params.id));

  app.get('/:id/occupancy', { schema: { params: idParams } }, async (request) => lotOccupancy(request.params.id));

  app.post('/', { schema: { body: createLotBody } }, async (request, reply) => {
    const lot = await createLot(request.body, app.rateCard.default);
    return reply.code(201).send(lot);
  });

  app.post('/:id/reservations', { schema: { params: idParams, body: reserveBayBody } }, async (request, reply) => {
    const plate = normalizePlate(request.body.plate);
    if (!plate) {
      return reply.code(422).send({ error: 'invalid plate' });
    }
    const reservation = await reserveBayForPlate(request.params.id, request.body.bayCode, plate);
    return reply.code(201).send(reservation);
  });
}

module.exports = lotRoutes;
