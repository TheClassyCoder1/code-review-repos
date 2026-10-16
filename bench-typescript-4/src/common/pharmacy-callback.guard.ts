import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHmac, timingSafeEqual } from 'node:crypto';

@Injectable()
export class PharmacyCallbackGuard implements CanActivate {
  constructor(private readonly config: ConfigService) {}

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();
    const provided: string | undefined = request.headers['x-rx-signature'];
    const rawBody: Buffer | undefined = request.rawBody;
    if (!provided || !rawBody) {
      throw new UnauthorizedException('Missing pharmacy signature');
    }
    const expected = createHmac('sha256', this.config.get<string>('pharmacy.callbackSecret')).update(rawBody).digest();
    const given = Buffer.from(provided, 'hex');
    if (given.length !== expected.length || !timingSafeEqual(given, expected)) {
      throw new UnauthorizedException('Invalid pharmacy signature');
    }
    return true;
  }
}
