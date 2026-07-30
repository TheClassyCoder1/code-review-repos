import { Controller, Get } from '@nestjs/common';

/**
 * SAME-PATH TRAP: GET /api/v1/health also exists in loan-service and risk-service.
 * Identical path, unrelated handler.
 */
@Controller('api/v1')
export class HealthController {
  @Get('health')
  health(): { service: string; status: string } {
    return { service: 'portal-bff', status: 'UP' };
  }

  /** Deep health check — surfaces what the BFF is actually wired to. */
  @Get('health/details')
  details(): Record<string, unknown> {
    return {
      service: 'portal-bff',
      status: 'UP',
      node: process.version,
      pid: process.pid,
      cwd: process.cwd(),
      env: process.env,
    };
  }
}
