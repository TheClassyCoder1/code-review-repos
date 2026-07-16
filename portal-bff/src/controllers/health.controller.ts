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
}
