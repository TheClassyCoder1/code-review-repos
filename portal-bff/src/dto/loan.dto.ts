// Portal-side loan DTOs. Mirror loan-service REST contracts (copied shapes, not shared types).
// These interfaces are structural only — no runtime behavior.

export interface LoanApplicationRequest {
  userId: number;
  amount: number;
  tier: string;
}

export interface RiskAssessment {
  loanId: number;
  score: number;
  decision: string;
}

// Raw loan record as returned by loan-service GET /api/v1/loans/{id}
export interface LoanRecord {
  id: number;
  userId: number;
  amount: number;
  tier: string;
  status: string;
}
