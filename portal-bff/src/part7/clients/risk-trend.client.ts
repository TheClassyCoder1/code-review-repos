import { Injectable } from '@nestjs/common';
import { RiskTrendRecord } from '../dto/repayment.dto';

/**
 * CROSS-REPO HTTP call (fetch): portal-bff -> risk-service GET /api/v1/risk/{id}/trend
 * (risk-service RiskTrendController.trend).
 */
@Injectable()
export class RiskTrendClient {
  private readonly baseUrl = process.env.RISK_SERVICE_URL ?? 'http://localhost:8082';

  async getTrend(loanId: number): Promise<RiskTrendRecord | null> {
    const res = await fetch(`${this.baseUrl}/api/v1/risk/${loanId}/trend`, {
      signal: AbortSignal.timeout(3000),
    });
    if (res.status === 404) {
      return null;
    }
    if (!res.ok) {
      throw new Error(`risk-service trend lookup failed with ${res.status}`);
    }
    return (await res.json()) as RiskTrendRecord;
  }
}
