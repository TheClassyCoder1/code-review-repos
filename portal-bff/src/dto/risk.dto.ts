// Risk DTO as returned by risk-service GET /api/v1/risk/{id} (copied shape).
export interface RiskDto {
  loanId: number;
  score: number;
  decision: string;
}
