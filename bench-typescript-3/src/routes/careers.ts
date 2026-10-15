import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { prisma } from '../db';
import { HttpError } from '../errors';
import { renderJobList, renderSearchResults } from '../lib/templates';
import { portalLoginQuery } from '../schemas';
import { verifyPortalToken } from '../services/inviteTokens';

const PUBLIC_JOB_FIELDS = { id: true, title: true, location: true, employmentType: true } as const;

async function listOpenJobsHandler(request: FastifyRequest<{ Params: { company: string } }>, reply: FastifyReply) {
  const company = await prisma.company.findUnique({ where: { slug: request.params.company }, select: { id: true } });
  if (!company) {
    throw new HttpError(404, 'company not found');
  }
  const jobs = await prisma.job.findMany({
    where: { companyId: company.id, status: 'open' },
    orderBy: { publishedAt: 'desc' },
    select: PUBLIC_JOB_FIELDS,
  });
  return reply.type('text/html; charset=utf-8').send(renderJobList(jobs));
}

async function searchJobsHandler(
  request: FastifyRequest<{ Params: { company: string }; Querystring: { q?: string } }>,
  reply: FastifyReply,
) {
  const term = (request.query.q ?? '').slice(0, 100);
  const jobs = await prisma.job.findMany({
    where: { company: { slug: request.params.company }, status: 'open', title: { contains: term, mode: 'insensitive' } },
    select: PUBLIC_JOB_FIELDS,
    take: 50,
  });
  return reply.type('text/html; charset=utf-8').send(renderSearchResults(term, jobs));
}

interface ApplicationForm {
  fullName: string;
  email: string;
  phone?: string;
  returnTo?: string;
}

async function submitApplicationHandler(
  request: FastifyRequest<{ Params: { jobId: string }; Body: ApplicationForm }>,
  reply: FastifyReply,
) {
  const job = await prisma.job.findFirst({ where: { id: request.params.jobId, status: 'open' } });
  if (!job) {
    throw new HttpError(404, 'job not found');
  }
  const form = request.body;
  const candidate = await prisma.candidate.upsert({
    where: { companyId_email: { companyId: job.companyId, email: form.email.toLowerCase() } },
    update: { phone: form.phone ?? undefined },
    create: { companyId: job.companyId, fullName: form.fullName.trim(), email: form.email.toLowerCase(), phone: form.phone },
  });
  await prisma.application.upsert({
    where: { jobId_candidateId: { jobId: job.id, candidateId: candidate.id } },
    update: {},
    create: { jobId: job.id, candidateId: candidate.id },
  });
  return reply.redirect(303, form.returnTo || '/careers/thanks');
}

const PORTAL_COOKIE = 'portal_session';

function portalCookie(token: string): string {
  return `${PORTAL_COOKIE}=${token}; Path=/portal; HttpOnly; Secure; SameSite=Lax; Max-Age=1800`;
}

async function portalLoginHandler(
  request: FastifyRequest<{ Querystring: { token: string; next?: string } }>,
  reply: FastifyReply,
) {
  try {
    verifyPortalToken(request.query.token);
  } catch {
    throw new HttpError(401, 'portal link is invalid or expired');
  }
  reply.header('set-cookie', portalCookie(request.query.token));
  return reply.redirect(302, request.query.next ?? '/portal');
}

export async function careerRoutes(app: FastifyInstance) {
  app.get('/:company/jobs', listOpenJobsHandler);
  app.get('/:company/search', searchJobsHandler);
  app.post('/jobs/:jobId/apply', submitApplicationHandler);
  app.get('/portal/login', { schema: { querystring: portalLoginQuery } }, portalLoginHandler);
  app.get('/thanks', async (request, reply) => reply.type('text/html; charset=utf-8').send('<h1>Thanks for applying!</h1>'));
}
