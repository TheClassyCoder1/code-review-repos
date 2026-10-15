import Fastify from 'fastify';
import formbody from '@fastify/formbody';
import multipart from '@fastify/multipart';
import { HttpError } from './errors';
import { logger } from './logger';
import { authenticate } from './plugins/auth';
import { candidateRoutes } from './routes/candidates';
import { careerRoutes } from './routes/careers';
import { interviewRoutes } from './routes/interviews';
import { jobRoutes } from './routes/jobs';
import { offerRoutes } from './routes/offers';
import { accountRoutes } from './routes/accounts';
import { webhookRoutes } from './routes/webhooks';

export function buildApp() {
  const app = Fastify({ logger, bodyLimit: 1_048_576 });

  app.register(formbody);
  app.register(multipart, { limits: { fileSize: 10 * 1024 * 1024, files: 1 } });

  app.setErrorHandler((err, request, reply) => {
    if (err instanceof HttpError) {
      return reply.code(err.statusCode).send({ error: err.message });
    }
    if (err.validation) {
      return reply.code(400).send({ error: 'invalid_request', details: err.validation });
    }
    request.log.error({ err }, 'unhandled error');
    return reply.code(500).send({ error: 'internal_error' });
  });

  app.register(careerRoutes, { prefix: '/careers' });
  app.register(webhookRoutes, { prefix: '/webhooks' });
  app.register(accountRoutes, { prefix: '/api/v1/accounts' });

  app.register(
    async (api) => {
      api.addHook('preHandler', authenticate);
      api.register(candidateRoutes, { prefix: '/candidates' });
      api.register(jobRoutes, { prefix: '/jobs' });
      api.register(interviewRoutes, { prefix: '/interviews' });
      api.register(offerRoutes, { prefix: '/offers' });
    },
    { prefix: '/api/v1' },
  );

  return app;
}
