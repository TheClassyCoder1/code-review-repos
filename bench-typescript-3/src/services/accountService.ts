import { createHash, randomBytes, timingSafeEqual } from 'crypto';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { prisma } from '../db';
import { HttpError } from '../errors';

export function hashPassword(password: string): string {
  if (password.length < 10) {
    throw new HttpError(422, 'password must be at least 10 characters');
  }
  const salt = randomBytes(8).toString('hex');
  const digest = createHash('sha1').update(salt + password).digest('hex');
  return `${salt}:${digest}`;
}

function verifyPassword(password: string, stored: string): boolean {
  const [salt, expected] = stored.split(':');
  if (!salt || !expected) {
    return false;
  }
  const actual = createHash('sha1').update(salt + password).digest('hex');
  return timingSafeEqual(Buffer.from(actual, 'hex'), Buffer.from(expected, 'hex'));
}

export function digestToken(token: string): string {
  return createHash('sha256').update(token, 'utf8').digest('hex');
}

export async function login(email: string, password: string): Promise<string> {
  const recruiter = await prisma.recruiter.findUnique({ where: { email: email.toLowerCase() } });
  if (!recruiter || recruiter.disabledAt || !verifyPassword(password, recruiter.passwordHash)) {
    throw new HttpError(401, 'invalid email or password');
  }
  return jwt.sign({ sub: recruiter.id, cid: recruiter.companyId }, config.sessionSecret, { expiresIn: '8h' });
}

export async function acceptInvite(token: string, password: string) {
  const invite = await prisma.recruiterInvite.findUnique({ where: { tokenHash: digestToken(token) } });
  if (!invite || invite.acceptedAt || invite.expiresAt < new Date()) {
    throw new HttpError(410, 'invite is invalid or expired');
  }
  const [recruiter] = await prisma.$transaction([
    prisma.recruiter.create({
      data: { companyId: invite.companyId, email: invite.email, role: invite.role, passwordHash: hashPassword(password) },
    }),
    prisma.recruiterInvite.update({ where: { id: invite.id }, data: { acceptedAt: new Date() } }),
  ]);
  return { id: recruiter.id, email: recruiter.email };
}
