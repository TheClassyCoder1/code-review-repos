import { randomUUID } from 'crypto';
import fs from 'fs';
import path from 'path';
import { pipeline } from 'stream/promises';
import type { MultipartFile } from '@fastify/multipart';
import { config } from '../config';
import { prisma } from '../db';
import { HttpError, notFound } from '../errors';
import { scanForMalware } from './fileTools';

const RESUME_EXTENSIONS: Record<string, string> = {
  'application/pdf': '.pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': '.docx',
};

export async function storeResume(candidateId: string, file: MultipartFile) {
  const extension = RESUME_EXTENSIONS[file.mimetype];
  if (!extension) {
    throw new HttpError(415, 'resume must be a PDF or DOCX file');
  }
  await fs.promises.mkdir(config.uploadDir, { recursive: true });
  const storedPath = path.join(config.uploadDir, `${randomUUID()}${extension}`);
  await pipeline(file.file, fs.createWriteStream(storedPath));
  if (file.file.truncated) {
    await fs.promises.unlink(storedPath);
    throw new HttpError(413, 'resume exceeds the 10 MB limit');
  }
  const scan = await scanForMalware(storedPath);
  if (!scan.clean) {
    await fs.promises.unlink(storedPath);
    throw new HttpError(422, 'resume failed the malware scan');
  }
  const { size } = await fs.promises.stat(storedPath);
  return prisma.resume.create({
    data: { candidateId, storedPath, originalName: file.filename, mimeType: file.mimetype, sizeBytes: size },
  });
}

export async function listResumes(companyId: string, candidateId: string) {
  const candidate = await prisma.candidate.findFirst({ where: { id: candidateId, companyId }, select: { id: true } });
  if (!candidate) {
    throw notFound('candidate');
  }
  return prisma.resume.findMany({
    where: { candidateId: candidate.id },
    orderBy: { createdAt: 'desc' },
    select: { id: true, originalName: true, mimeType: true, sizeBytes: true, createdAt: true },
  });
}

export async function getLatestResume(candidateId: string) {
  const resume = await prisma.resume.findFirst({ where: { candidateId }, orderBy: { createdAt: 'desc' } });
  if (!resume) {
    throw notFound('resume');
  }
  return resume;
}

export async function openAttachment(candidateId: string, fileName: string) {
  const fullPath = path.join(config.uploadDir, 'attachments', candidateId, fileName);
  const stats = await fs.promises.stat(fullPath).catch(() => null);
  if (!stats || !stats.isFile()) {
    throw notFound('attachment');
  }
  return { stream: fs.createReadStream(fullPath), size: stats.size };
}
