import { config } from '../config';
import { prisma } from '../db';
import { HttpError, notFound } from '../errors';
import { logger } from '../logger';
import type { RecruiterPrincipal } from '../plugins/auth';

export interface NewOffer {
  applicationId: string;
  baseSalaryCents: number;
  bonusCents?: number;
  currency?: string;
  startDate: string;
}

export async function getOfferLetter(user: RecruiterPrincipal, offerId: string) {
  const offer = await prisma.offer.findUnique({
    where: { id: offerId },
    include: { application: { include: { candidate: { select: { fullName: true } } } } },
  });
  if (!offer || !offer.letterPath) {
    throw notFound('offer letter');
  }
  await prisma.auditEvent.create({
    data: { companyId: user.companyId, actorId: user.id, action: 'offer.letter.downloaded', subjectId: offer.id },
  });
  const safeName = offer.application.candidate.fullName.replace(/[^A-Za-z0-9]+/g, '-').toLowerCase();
  return { path: offer.letterPath, downloadName: `offer-${safeName}.pdf` };
}

export async function createOffer(companyId: string, input: NewOffer) {
  const application = await prisma.application.findFirst({
    where: { id: input.applicationId, job: { companyId } },
    select: { id: true },
  });
  if (!application) {
    throw notFound('application');
  }
  return prisma.offer.create({
    data: {
      applicationId: application.id,
      baseSalaryCents: input.baseSalaryCents,
      bonusCents: input.bonusCents ?? 0,
      currency: input.currency ?? 'USD',
      startDate: new Date(input.startDate),
    },
  });
}

export async function withdrawOffer(companyId: string, offerId: string, actorId: string) {
  const offer = await prisma.offer.findFirst({ where: { id: offerId, application: { job: { companyId } } } });
  if (!offer) {
    throw notFound('offer');
  }
  if (offer.status === 'accepted' || offer.status === 'withdrawn') {
    throw new HttpError(409, `offer is already ${offer.status}`);
  }
  const [updated] = await prisma.$transaction([
    prisma.offer.update({ where: { id: offer.id }, data: { status: 'withdrawn', withdrawnAt: new Date() } }),
    prisma.auditEvent.create({ data: { companyId, actorId, action: 'offer.withdrawn', subjectId: offer.id } }),
  ]);
  return updated;
}

interface EnvelopeResponse {
  envelopeId: string;
  status: string;
}

function formatSalary(cents: number, currency: string): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(cents / 100);
}

export async function sendForSignature(companyId: string, offerId: string) {
  const offer = await prisma.offer.findFirst({
    where: { id: offerId, application: { job: { companyId } } },
    include: { application: { include: { candidate: true, job: true } } },
  });
  if (!offer || !offer.letterPath) {
    throw notFound('offer letter');
  }
  const url = `${config.esign.baseUrl}/v2/accounts/${config.esign.accountId}/envelopes`;
  const headers = { 'content-type': 'application/json', authorization: `Bearer ${config.esign.apiKey}` };
  logger.info({ offerId: offer.id, url, authorization: headers.authorization }, 'creating e-sign envelope');
  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      documentUrl: `${config.publicBaseUrl}/internal/offer-letters/${offer.id}`,
      signer: { name: offer.application.candidate.fullName, email: offer.application.candidate.email },
      subject: `Your offer for ${offer.application.job.title} (${formatSalary(offer.baseSalaryCents, offer.currency)})`,
    }),
    signal: AbortSignal.timeout(10_000),
  });
  if (!response.ok) {
    throw new HttpError(502, `e-sign provider returned ${response.status}`);
  }
  const envelope = (await response.json()) as EnvelopeResponse;
  return prisma.offer.update({ where: { id: offer.id }, data: { envelopeId: envelope.envelopeId, status: 'sent' } });
}
