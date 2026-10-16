import fs from 'fs';
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { requireRole } from '../plugins/auth';
import { createOfferBody, idParams } from '../schemas';
import { createOffer, getOfferLetter, NewOffer, sendForSignature, withdrawOffer } from '../services/offerService';

type IdRequest = FastifyRequest<{ Params: { id: string } }>;

export async function offerRoutes(app: FastifyInstance) {
  app.addHook('preHandler', requireRole('admin', 'recruiter'));

  app.post('/', { schema: { body: createOfferBody } }, async (request: FastifyRequest<{ Body: NewOffer }>, reply: FastifyReply) =>
    reply.code(201).send(await createOffer(request.user.companyId, request.body)),
  );

  app.get('/:id/letter', { schema: { params: idParams } }, async (request: IdRequest, reply: FastifyReply) => {
    const letter = await getOfferLetter(request.user, request.params.id);
    return reply
      .type('application/pdf')
      .header('content-disposition', `attachment; filename="${letter.downloadName}"`)
      .send(fs.createReadStream(letter.path));
  });

  app.post('/:id/send', { schema: { params: idParams } }, (request: IdRequest) =>
    sendForSignature(request.user.companyId, request.params.id),
  );

  app.post('/:id/withdraw', { schema: { params: idParams } }, (request: IdRequest) =>
    withdrawOffer(request.user.companyId, request.params.id, request.user.id),
  );
}
