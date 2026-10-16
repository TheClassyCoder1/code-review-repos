import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { authenticate, requireRole } from '../plugins/auth';
import { acceptInvite, login } from '../services/accountService';
import { createRecruiterInvite } from '../services/inviteTokens';

const loginBody = {
  type: 'object',
  required: ['email', 'password'],
  properties: { email: { type: 'string', format: 'email' }, password: { type: 'string', minLength: 1, maxLength: 200 } },
} as const;

const inviteBody = {
  type: 'object',
  required: ['email', 'role'],
  properties: { email: { type: 'string', format: 'email' }, role: { type: 'string', enum: ['admin', 'recruiter', 'interviewer'] } },
} as const;

export async function accountRoutes(app: FastifyInstance) {
  app.post('/login', { schema: { body: loginBody } }, async (request: FastifyRequest<{ Body: { email: string; password: string } }>) => ({
    token: await login(request.body.email, request.body.password),
  }));

  app.post('/invites', { schema: { body: inviteBody }, preHandler: [authenticate, requireRole('admin')] }, async (request: FastifyRequest<{ Body: { email: string; role: string } }>, reply: FastifyReply) =>
    reply.code(201).send(await createRecruiterInvite(request.user.companyId, request.body.email, request.body.role)),
  );

  app.post('/invites/accept', async (request: FastifyRequest<{ Body: { token: string; password: string } }>, reply: FastifyReply) =>
    reply.code(201).send(await acceptInvite(request.body.token, request.body.password)),
  );
}
