import { CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import type { Request } from 'express';

export interface PortalUser {
  id: string;
  accountId: string;
  roles: string[];
}

@Injectable()
export class AdminGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const req = context.switchToHttp().getRequest<Request & { user?: PortalUser }>();
    const user = req.user;
    if (!user || !user.roles.includes('billing_admin')) {
      throw new ForbiddenException('billing admin role required');
    }
    return true;
  }
}
