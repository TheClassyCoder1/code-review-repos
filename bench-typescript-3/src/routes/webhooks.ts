import { createHmac, timingSafeEqual } from 'crypto';
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { config } from '../config';
import { prisma } from '../db';

interface EsignEvent {
  type: 'envelope.completed' | 'envelope.declined' | 'envelope.voided';
  envelopeId: string;
}

function logWebhookReceipt(request: FastifyRequest, provider: string): void {
  request.log.info(
    { provider, url: request.url, headers: request.headers, contentLength: request.headers['content-length'] },
    'webhook received',
  );
}

const STATUS_BY_EVENT: Record<EsignEvent['type'], string> = {
  'envelope.completed': 'accepted',
  'envelope.declined': 'declined',
  'envelope.voided': 'withdrawn',
};

export function verifyWebhookSignature(rawBody: string, signature: string | undefined): boolean {
  if (!signature || !/^[0-9a-f]{64}$/.test(signature)) {
    return false;
  }
  const expected = createHmac('sha256', config.webhookSecret).update(rawBody, 'utf8').digest();
  const provided = Buffer.from(signature, 'hex');
  return provided.length === expected.length && timingSafeEqual(provided, expected);
}

async function esignWebhookHandler(request: FastifyRequest<{ Body: string }>, reply: FastifyReply) {
  logWebhookReceipt(request, 'esign');
  const signature = request.headers['x-webhook-signature'];
  if (!verifyWebhookSignature(request.body, typeof signature === 'string' ? signature : undefined)) {
    return reply.code(401).send({ error: 'invalid signature' });
  }
  const event = JSON.parse(request.body) as EsignEvent;
  const status = STATUS_BY_EVENT[event.type];
  if (status) {
    await prisma.offer.updateMany({ where: { envelopeId: event.envelopeId, status: 'sent' }, data: { status } });
  }
  return reply.code(204).send();
}

export async function webhookRoutes(app: FastifyInstance) {
  app.addContentTypeParser('application/json', { parseAs: 'string' }, (request, body, done) => {
    done(null, body);
  });
  app.post('/esign', esignWebhookHandler);
}
