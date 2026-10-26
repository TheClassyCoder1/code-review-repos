import { RiskDto } from './risk.dto';

// Review band risk-service adds to GET /api/v1/risk/{id}. Optional: older
// risk-service builds do not send it, so callers must treat it as absent.
export type ReviewBand = 'AUTO' | 'MANUAL' | 'DECLINE';

export interface RiskWithReviewBand extends RiskDto {
  reviewBand?: ReviewBand;
}

export function isAwaitingUnderwriter(risk: RiskWithReviewBand): boolean {
  return risk.reviewBand === 'MANUAL';
}
