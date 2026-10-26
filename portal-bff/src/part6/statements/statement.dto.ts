// Shapes returned by loan-service GET /api/v1/loans/{id}/statement and the portal view built from it.

export interface LoanStatementLine {
  postedOn: string;
  description: string;
  amount: number;
}

export interface LoanStatement {
  loanId: number;
  periodStart: string;
  periodEnd: string;
  openingBalance: number;
  closingBalance: number;
  minimumPayment: number;
  dueDate: string;
  lines: LoanStatementLine[];
}

export interface PortalStatementView {
  loanId: number;
  period: string;
  balance: string;
  minimumDue: string;
  dueOn: string;
  daysUntilDue: number;
  overdue: boolean;
  lineCount: number;
}
