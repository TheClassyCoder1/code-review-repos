/**
 * Shapes returned by loan-service /api/v1/loans/{id}/schedule* and risk-service /api/v1/risk/{id}/trend.
 * BigDecimal fields arrive as JSON numbers.
 */
export interface InstallmentRecord {
  sequenceNo: number;
  dueDate: string;
  principal: number;
  interest: number;
  total: number;
  remainingBalance: number;
  status: 'DUE' | 'PAID' | 'SUPERSEDED';
}

export interface RepaymentScheduleRecord {
  loanId: number;
  installmentCount: number;
  totalPrincipal: number;
  totalInterest: number;
  outstanding: number;
  installments: InstallmentRecord[];
}

export interface PayoffQuoteRecord {
  loanId: number;
  payoffDate: string;
  outstandingPrincipal: number;
  accruedInterest: number;
  prepaymentFee: number;
  payoffAmount: number;
  validUntil: string;
}

export interface RiskTrendRecord {
  loanId: number;
  observations: number;
  latestScore: number;
  averageScore: number;
  slope: number;
  direction: 'RISING' | 'FALLING' | 'STABLE';
  watchlisted: boolean;
  recentScores: number[];
}

/** What the portal's "My repayments" page renders. */
export interface RepaymentOverview {
  loanId: number;
  nextDue: { dueDate: string; amount: number } | null;
  paidCount: number;
  remainingCount: number;
  outstanding: number;
  progressPercent: number;
  overdue: boolean;
  riskDirection: RiskTrendRecord['direction'] | 'UNKNOWN';
}
