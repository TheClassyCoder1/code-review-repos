import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHmac, timingSafeEqual } from 'node:crypto';

@Injectable()
export class PatientAuthGuard implements CanActivate {
  constructor(private readonly config: ConfigService) {}

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();
    const header: string | undefined = request.headers['authorization'];
    if (!header?.startsWith('Bearer ')) {
      throw new UnauthorizedException();
    }
    const [payload, signature] = header.slice(7).split('.');
    if (!payload || !signature) {
      throw new UnauthorizedException();
    }
    const expected = createHmac('sha256', this.config.get<string>('PATIENT_SESSION_SECRET')).update(payload).digest();
    const given = Buffer.from(signature, 'base64url');
    if (given.length !== expected.length || !timingSafeEqual(given, expected)) {
      throw new UnauthorizedException();
    }
    const session = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8')) as { sub: string; exp: number };
    if (session.exp * 1000 < Date.now()) {
      throw new UnauthorizedException();
    }
    request.patient = { id: session.sub };
    return true;
  }
}
