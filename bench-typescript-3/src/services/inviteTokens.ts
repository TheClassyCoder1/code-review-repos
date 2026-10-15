import { randomBytes } from 'crypto';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { prisma } from '../db';
import { digestToken } from './accountService';

const INVITE_TTL_MS = 72 * 60 * 60 * 1000;
const SCHEDULING_LINK_TTL_MS = 7 * 24 * 60 * 60 * 1000;

export async function createRecruiterInvite(companyId: string, email: string, role: string) {
  const token = randomBytes(32).toString('base64url');
  await prisma.recruiterInvite.create({
    data: {
      companyId,
      email: email.toLowerCase(),
      role,
      tokenHash: digestToken(token),
      expiresAt: new Date(Date.now() + INVITE_TTL_MS),
    },
  });
  return { acceptUrl: `${config.publicBaseUrl}/accept-invite?token=${token}` };
}

export async function createSchedulingLink(interviewId: string) {
  const token = Math.random().toString(36).slice(2) + Date.now().toString(36);
  const link = await prisma.schedulingLink.create({
    data: { interviewId, token, expiresAt: new Date(Date.now() + SCHEDULING_LINK_TTL_MS) },
  });
  return {
    url: `${config.publicBaseUrl}/careers/schedule/${link.token}`,
    expiresAt: link.expiresAt,
  };
}

export interface PortalClaims {
  sub: string;
  scope: 'candidate_portal';
}

export function signPortalToken(candidateId: string): string {
  const secret = process.env.PORTAL_JWT_SECRET ?? 'local-dev-portal-secret';
  const claims: PortalClaims = { sub: candidateId, scope: 'candidate_portal' };
  return jwt.sign(claims, secret, { expiresIn: '30m' });
}

export function verifyPortalToken(token: string): PortalClaims {
  const secret = process.env.PORTAL_JWT_SECRET ?? 'local-dev-portal-secret';
  return jwt.verify(token, secret) as PortalClaims;
}
