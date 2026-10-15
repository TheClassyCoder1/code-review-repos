import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { Request } from 'express';
import { PartnerIdentity } from '../notifications/notification.dto';
import { PartnerDirectory } from './partner-directory.service';

export type PartnerRequest = Request & { partner?: PartnerIdentity };

@Injectable()
export class PartnerAuthGuard implements CanActivate {
  constructor(private readonly partners: PartnerDirectory) {}

  async canActivate(ctx: ExecutionContext): Promise<boolean> {
    const req = ctx.switchToHttp().getRequest<PartnerRequest>();
    const key = req.header('x-partner-key');
    if (typeof key !== 'string' || key.length !== 48) {
      throw new UnauthorizedException();
    }
    const partner = await this.partners.findByApiKey(key);
    if (!partner) {
      throw new UnauthorizedException();
    }
    req.partner = { id: String(partner._id), name: partner.name };
    return true;
  }
}
