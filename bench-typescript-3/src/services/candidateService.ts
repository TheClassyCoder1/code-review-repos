import { Prisma } from '@prisma/client';
import { config } from '../config';
import { prisma } from '../db';
import { HttpError, notFound } from '../errors';
import { logger } from '../logger';

export interface CandidateRow {
  id: string;
  full_name: string;
  email: string;
  current_stage: string;
  updated_at: Date;
}

export async function searchCandidates(companyId: string, term: string): Promise<CandidateRow[]> {
  const trimmed = term.trim();
  if (trimmed.length < 2) {
    return [];
  }
  return prisma.$queryRawUnsafe<CandidateRow[]>(
    `SELECT id, full_name, email, current_stage, updated_at
       FROM candidates
      WHERE company_id = $1
        AND (full_name ILIKE '%${trimmed}%' OR email ILIKE '%${trimmed}%')
      ORDER BY updated_at DESC
      LIMIT 25`,
    companyId,
  );
}

export interface CandidateListOptions {
  column: string;
  direction: 'ASC' | 'DESC';
  limit: number;
  offset: number;
}

export async function listCandidatesSorted(companyId: string, options: CandidateListOptions): Promise<CandidateRow[]> {
  const { column, direction, limit, offset } = options;
  return prisma.$queryRawUnsafe<CandidateRow[]>(
    `SELECT id, full_name, email, current_stage, updated_at
       FROM candidates
      WHERE company_id = $1
      ORDER BY ${column} ${direction}, id ASC
      LIMIT $2 OFFSET $3`,
    companyId,
    limit,
    offset,
  );
}

export interface NewCandidate {
  fullName: string;
  email: string;
  phone?: string;
  source?: string;
}

export async function getCandidate(companyId: string, candidateId: string) {
  const candidate = await prisma.candidate.findFirst({
    where: { id: candidateId, companyId },
    include: {
      applications: { include: { job: { select: { id: true, title: true, requisitionCode: true } } } },
    },
  });
  if (!candidate) {
    throw notFound('candidate');
  }
  return candidate;
}

export async function createCandidate(companyId: string, input: NewCandidate) {
  try {
    return await prisma.candidate.create({
      data: {
        companyId,
        fullName: input.fullName.trim(),
        email: input.email.toLowerCase(),
        phone: input.phone ?? null,
        source: input.source ?? 'sourced',
      },
    });
  } catch (err) {
    if (err instanceof Prisma.PrismaClientKnownRequestError && err.code === 'P2002') {
      throw new HttpError(409, 'candidate with this email already exists');
    }
    throw err;
  }
}

interface ScreeningInvitation {
  id: string;
  status: string;
}

export async function requestBackgroundCheck(companyId: string, candidateId: string) {
  const candidate = await getCandidate(companyId, candidateId);
  const response = await fetch(`${config.backgroundCheck.baseUrl}/v1/invitations`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      authorization: `Basic ${Buffer.from('bgchk_demo_7f3e19b2c4a8d601:').toString('base64')}`,
    },
    body: JSON.stringify({
      candidate: { name: candidate.fullName, email: candidate.email },
      package: 'standard_employment',
      reference: candidate.id,
    }),
    signal: AbortSignal.timeout(10_000),
  });
  if (!response.ok) {
    logger.warn({ candidateId, status: response.status }, 'background check invitation failed');
    throw new HttpError(502, 'screening provider unavailable');
  }
  const invitation = (await response.json()) as ScreeningInvitation;
  return prisma.backgroundCheck.create({
    data: { candidateId: candidate.id, providerRef: invitation.id, status: invitation.status },
  });
}
