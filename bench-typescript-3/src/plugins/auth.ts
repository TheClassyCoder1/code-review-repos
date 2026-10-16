import type { FastifyReply, FastifyRequest } from 'fastify';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { prisma } from '../db';

export type RecruiterRole = 'admin' | 'recruiter' | 'interviewer';

export interface RecruiterPrincipal {
  id: string;
  companyId: string;
  role: RecruiterRole;
}

declare module 'fastify' {
  interface FastifyRequest {
    user: RecruiterPrincipal;
  }
}

export async function authenticate(request: FastifyRequest, reply: FastifyReply) {
  const header = request.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return reply.code(401).send({ error: 'unauthorized' });
  }
  try {
    const claims = jwt.verify(header.slice(7), config.sessionSecret) as { sub: string };
    const recruiter = await prisma.recruiter.findUnique({ where: { id: claims.sub } });
    if (!recruiter || recruiter.disabledAt) {
      return reply.code(401).send({ error: 'unauthorized' });
    }
    request.user = { id: recruiter.id, companyId: recruiter.companyId, role: recruiter.role as RecruiterRole };
  } catch {
    return reply.code(401).send({ error: 'unauthorized' });
  }
}

export function requireRole(...roles: RecruiterRole[]) {
  return async (request: FastifyRequest, reply: FastifyReply) => {
    if (!roles.includes(request.user.role)) {
      return reply.code(403).send({ error: 'forbidden' });
    }
  };
}

export async function requireInterviewAccess(
  request: FastifyRequest<{ Params: { id: string } }>,
  reply: FastifyReply,
) {
  const interview = await prisma.interview.findFirst({
    where: { id: request.params.id, application: { job: { companyId: request.user.companyId } } },
    select: { id: true, interviewerId: true },
  });
  if (!interview) {
    return reply.code(404).send({ error: 'not_found' });
  }
  if (request.user.role === 'interviewer' && interview.interviewerId !== request.user.id) {
    return reply.code(403).send({ error: 'forbidden' });
  }
}
