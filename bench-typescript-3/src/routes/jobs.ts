import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { requireRole } from '../plugins/auth';
import { idParams, importJobBody } from '../schemas';
import { importJobFromUrl, importJobsFromYaml } from '../services/jobFeeds';
import { createJob, JobInput, loadPipelineTemplate, publishJob } from '../services/jobService';

export async function jobRoutes(app: FastifyInstance) {
  app.addContentTypeParser(['application/yaml', 'text/yaml'], { parseAs: 'string' }, (request, body, done) => {
    done(null, body);
  });

  app.get('/pipeline', async () => ({ stages: loadPipelineTemplate() }));

  app.post('/', { preHandler: requireRole('admin', 'recruiter') }, async (request: FastifyRequest<{ Body: JobInput }>, reply: FastifyReply) =>
    reply.code(201).send(await createJob(request.user.companyId, request.body)),
  );

  app.post('/:id/publish', { schema: { params: idParams }, preHandler: requireRole('admin', 'recruiter') }, (request: FastifyRequest<{ Params: { id: string } }>) =>
    publishJob(request.user.companyId, request.params.id),
  );

  app.post('/import', { schema: { body: importJobBody }, preHandler: requireRole('admin') }, async (request: FastifyRequest<{ Body: { sourceUrl: string } }>, reply: FastifyReply) =>
    reply.code(201).send(await importJobFromUrl(request.user.companyId, request.body.sourceUrl)),
  );

  app.post('/import/yaml', { preHandler: requireRole('admin') }, (request: FastifyRequest<{ Body: string }>) =>
    importJobsFromYaml(request.user.companyId, request.body),
  );
}
