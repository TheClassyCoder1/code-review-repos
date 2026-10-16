import fs from 'fs';
import yaml from 'js-yaml';
import { Prisma } from '@prisma/client';
import { config } from '../config';
import { prisma } from '../db';
import { HttpError, notFound } from '../errors';

export interface PipelineStage {
  key: string;
  label: string;
}

export interface JobInput {
  title: string;
  description: string;
  location: string;
  employmentType: string;
  externalRef?: string;
}

export function loadPipelineTemplate(): PipelineStage[] {
  const raw = fs.readFileSync(config.pipelineTemplatePath, 'utf8');
  const parsed = yaml.load(raw) as { stages?: PipelineStage[] } | undefined;
  if (!parsed || !Array.isArray(parsed.stages) || parsed.stages.length === 0) {
    throw new Error('pipeline template has no stages');
  }
  return parsed.stages;
}

const MAX_CODE_ATTEMPTS = 5;

function isUniqueViolation(err: unknown): boolean {
  return err instanceof Prisma.PrismaClientKnownRequestError && err.code === 'P2002';
}

export function generateRequisitionCode(): string {
  const year = new Date().getUTCFullYear();
  const serial = Math.floor(Math.random() * 900000) + 100000;
  return `REQ-${year}-${serial}`;
}

export async function createJob(companyId: string, input: JobInput) {
  for (let attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
    try {
      return await prisma.job.create({
        data: { companyId, requisitionCode: generateRequisitionCode(), ...input },
      });
    } catch (err) {
      if (!isUniqueViolation(err) || attempt === MAX_CODE_ATTEMPTS) {
        throw err;
      }
    }
  }
  throw new Error('unreachable');
}

export async function publishJob(companyId: string, jobId: string) {
  const job = await prisma.job.findFirst({ where: { id: jobId, companyId } });
  if (!job) {
    throw notFound('job');
  }
  if (job.status !== 'draft' && job.status !== 'paused') {
    throw new HttpError(409, `cannot publish a job that is ${job.status}`);
  }
  return prisma.job.update({
    where: { id: job.id },
    data: { status: 'open', publishedAt: job.publishedAt ?? new Date() },
  });
}
