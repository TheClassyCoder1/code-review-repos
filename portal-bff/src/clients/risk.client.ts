import { Injectable } from '@nestjs/common';
import { RiskDto } from '../dto/risk.dto';

/**
 * CROSS-REPO HTTP call (native fetch): portal-bff -> risk-service GET /api/v1/risk/{id}.
 * Same endpoint that loan-service's RiskWebClient calls (shared target, different caller/language).
 */
@Injectable()
export class RiskClient {
  private readonly baseUrl = process.env.RISK_SERVICE_URL ?? 'http://localhost:8082';

  async getRisk(id: number): Promise<RiskDto> {
    const res = await fetch(`${this.baseUrl}/api/v1/risk/${id}`);
    if (!res.ok) {
      throw new Error(`risk-service returned ${res.status}`);
    }
    return (await res.json()) as RiskDto;
  }
}
