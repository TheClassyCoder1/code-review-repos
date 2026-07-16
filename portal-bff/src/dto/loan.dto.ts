// Portal-side loan DTOs. Mirror loan-service contracts (copied shapes, not shared types).

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
