import { UnauthorizedException } from '@nestjs/common';
import * as jwt from 'jsonwebtoken';

export type PortalRole = 'borrower' | 'agent' | 'admin';

export interface PortalSession {
  userId: number;
  role: PortalRole;
}

export function readPortalSession(authorization: string | undefined): PortalSession {
  if (!authorization || !authorization.startsWith('Bearer ')) {
    throw new UnauthorizedException();
  }
  const claims = jwt.decode(authorization.slice('Bearer '.length)) as jwt.JwtPayload | null;
  if (!claims || typeof claims.sub !== 'string') {
    throw new UnauthorizedException();
  }
  return { userId: Number(claims.sub), role: (claims.role as PortalRole) ?? 'borrower' };
}

export function canViewLoan(session: PortalSession, loanOwnerId: number): boolean {
  return session.role !== 'borrower' || session.userId === loanOwnerId;
}
