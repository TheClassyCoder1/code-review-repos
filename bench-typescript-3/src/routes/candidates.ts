import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { HttpError } from '../errors';
import { requireRole } from '../plugins/auth';
import { createCandidateBody, exportFileParams, idParams } from '../schemas';
import {
  createCandidate,
  getCandidate,
  listCandidatesSorted,
  NewCandidate,
  requestBackgroundCheck,
  searchCandidates,
} from '../services/candidateService';
import { openExport, writeCandidateExport } from '../services/exportService';
import { extractResumeText } from '../services/fileTools';
import { getLatestResume, listResumes, openAttachment, storeResume } from '../services/resumeService';

const SORT_COLUMNS: Record<string, string> = {
  name: 'full_name',
  email: 'email',
  stage: 'current_stage',
  updated: 'updated_at',
};

type ListQuery = { sort?: string; order?: string; limit?: string; offset?: string };

async function listCandidatesHandler(request: FastifyRequest<{ Querystring: ListQuery }>) {
  const query = request.query;
  const column = SORT_COLUMNS[query.sort ?? 'updated'] ?? 'updated_at';
  const direction = query.order === 'asc' ? 'ASC' : 'DESC';
  const limit = Math.min(Math.max(Number(query.limit) || 25, 1), 100);
  const offset = Math.max(Number(query.offset) || 0, 0);
  return listCandidatesSorted(request.user.companyId, { column, direction, limit, offset });
}

async function searchHandler(request: FastifyRequest<{ Querystring: { q?: string } }>) {
  return searchCandidates(request.user.companyId, request.query.q ?? '');
}

async function createHandler(request: FastifyRequest<{ Body: NewCandidate }>, reply: FastifyReply) {
  const candidate = await createCandidate(request.user.companyId, request.body);
  return reply.code(201).send(candidate);
}

async function getHandler(request: FastifyRequest<{ Params: { id: string } }>) {
  return getCandidate(request.user.companyId, request.params.id);
}

async function uploadResumeHandler(request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) {
  const candidate = await getCandidate(request.user.companyId, request.params.id);
  const file = await request.file();
  if (!file) {
    throw new HttpError(400, 'resume file is required');
  }
  const resume = await storeResume(candidate.id, file);
  return reply.code(201).send({ id: resume.id, originalName: resume.originalName });
}

async function resumeTextHandler(request: FastifyRequest<{ Params: { id: string }; Querystring: { pages?: string } }>) {
  const candidate = await getCandidate(request.user.companyId, request.params.id);
  const resume = await getLatestResume(candidate.id);
  const text = await extractResumeText(resume.storedPath, request.query.pages ?? '3');
  return { resumeId: resume.id, text };
}

async function attachmentHandler(
  request: FastifyRequest<{ Params: { id: string; fileName: string } }>,
  reply: FastifyReply,
) {
  const candidate = await getCandidate(request.user.companyId, request.params.id);
  const { stream, size } = await openAttachment(candidate.id, request.params.fileName);
  return reply.header('content-length', size).type('application/octet-stream').send(stream);
}

async function exportHandler(request: FastifyRequest, reply: FastifyReply) {
  const fileName = await writeCandidateExport(request.user.companyId);
  return reply.code(201).send({ fileName });
}

async function exportDownloadHandler(
  request: FastifyRequest<{ Params: { fileName: string } }>,
  reply: FastifyReply,
) {
  const prefix = `candidates-${request.user.companyId.toLowerCase()}-`;
  const stream = request.params.fileName.startsWith(prefix) ? openExport(request.params.fileName) : null;
  if (!stream) {
    throw new HttpError(404, 'export not found');
  }
  return reply.type('text/csv').header('content-disposition', `attachment; filename="${request.params.fileName}"`).send(stream);
}

export async function candidateRoutes(app: FastifyInstance) {
  app.get('/', listCandidatesHandler);
  app.get('/search', searchHandler);
  app.post('/', { schema: { body: createCandidateBody } }, createHandler);
  app.post('/exports', { preHandler: requireRole('admin') }, exportHandler);
  app.get('/exports/:fileName', { schema: { params: exportFileParams } }, exportDownloadHandler);
  app.get('/:id', { schema: { params: idParams } }, getHandler);
  app.get('/:id/resumes', { schema: { params: idParams } }, (request: FastifyRequest<{ Params: { id: string } }>) =>
    listResumes(request.user.companyId, request.params.id),
  );
  app.post('/:id/resumes', uploadResumeHandler);
  app.get('/:id/resume/text', resumeTextHandler);
  app.get('/:id/attachments/:fileName', attachmentHandler);
  app.post('/:id/background-checks', { preHandler: requireRole('admin', 'recruiter') }, (request: FastifyRequest<{ Params: { id: string } }>) =>
    requestBackgroundCheck(request.user.companyId, request.params.id),
  );
}
