import { LoanRecord } from './loan.dto';
import { RiskDto } from './risk.dto';

// Aggregated view merged from loan-service + risk-service.
export interface Dashboard {
  userId: number;
  loan: LoanRecord | null;
  risk: RiskDto | null;
}

// Transformed (reshaped) loan view returned by the transforming proxy.
export interface PortalLoanView {
  loanId: number;
  borrower: number;
  requestedAmount: number;
  state: string;
}
