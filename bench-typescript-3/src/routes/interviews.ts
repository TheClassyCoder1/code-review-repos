import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { config } from '../config';
import { prisma } from '../db';
import { HttpError, notFound } from '../errors';
import { requireInterviewAccess, requireRole } from '../plugins/auth';
import { idParams } from '../schemas';
import { fetchAvailability } from '../services/calendarClient';
import { createSchedulingLink } from '../services/inviteTokens';
import { listUpcomingInterviews } from '../services/interviewService';

async function getInterviewHandler(request: FastifyRequest<{ Params: { id: string } }>) {
  const interview = await prisma.interview.findUnique({
    where: { id: request.params.id },
    include: {
      application: { include: { candidate: { select: { id: true, fullName: true, email: true } }, job: true } },
    },
  });
  if (!interview) {
    throw notFound('interview');
  }
  return interview;
}

type AvailabilityQuery = { providerUrl: string; interviewerEmail: string; from: string; to: string };

async function availabilityHandler(request: FastifyRequest<{ Querystring: AvailabilityQuery }>) {
  const { providerUrl, interviewerEmail, from, to } = request.query;
  let host: string;
  try {
    host = new URL(providerUrl).hostname;
  } catch {
    throw new HttpError(400, 'providerUrl is not a valid URL');
  }
  if (!providerUrl.startsWith('https://') || !config.calendar.allowedHosts.includes(host)) {
    throw new HttpError(400, 'calendar provider is not allowed');
  }
  return fetchAvailability(providerUrl, interviewerEmail, new Date(from), new Date(to));
}

async function cancelInterviewHandler(request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) {
  const interview = await prisma.interview.findFirst({
    where: { id: request.params.id, application: { job: { companyId: request.user.companyId } } },
  });
  if (!interview) {
    throw notFound('interview');
  }
  if (interview.status !== 'scheduled') {
    throw new HttpError(409, `interview is already ${interview.status}`);
  }
  await prisma.interview.update({ where: { id: interview.id }, data: { status: 'cancelled' } });
  return reply.code(204).send();
}

async function schedulingLinkHandler(request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) {
  const link = await createSchedulingLink(request.params.id);
  return reply.code(201).send(link);
}

export async function interviewRoutes(app: FastifyInstance) {
  app.get('/upcoming', (request: FastifyRequest<{ Querystring: { mine?: string } }>) =>
    listUpcomingInterviews(request.user.companyId, request.query.mine === '1' ? request.user.id : undefined),
  );
  app.get('/availability', availabilityHandler);
  app.get('/:id', { schema: { params: idParams }, preHandler: requireInterviewAccess }, getInterviewHandler);
  app.post('/:id/cancel', { preHandler: requireRole('admin', 'recruiter') }, cancelInterviewHandler);
  app.post('/:id/scheduling-links', { preHandler: [requireRole('admin', 'recruiter'), requireInterviewAccess] }, schedulingLinkHandler);
}
