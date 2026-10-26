/** Amounts are always carried in minor units (cents) to avoid float drift. */
export type MinorUnits = number;

export type CurrencyCode = 'USD' | 'EUR' | 'GBP' | 'INR';

export type DisbursementStatus =
  | 'REQUESTED'
  | 'RESERVED'
  | 'SUBMITTED'
  | 'SETTLED'
  | 'FAILED'
  | 'CANCELLED';

export interface DisbursementRequest {
  requestId: string;
  loanId: number;
  accountRef: string;
  amountMinor: MinorUnits;
  currency: CurrencyCode;
  requestedAt: string;
}

export interface DisbursementRecord {
  disbursementId: string;
  loanId: number;
  accountRef: string;
  amountMinor: MinorUnits;
  currency: CurrencyCode;
  status: DisbursementStatus;
  gatewayReference: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Installment {
  sequence: number;
  dueDate: string;
  principalMinor: MinorUnits;
  interestMinor: MinorUnits;
  balanceAfterMinor: MinorUnits;
}

export interface RepaymentEntry {
  loanId: number;
  dueDate: string;
  amountDueMinor: MinorUnits;
  amountPaidMinor: MinorUnits;
  paidOn: string | null;
}

export interface StatementLine {
  postedOn: string;
  description: string;
  debitMinor: MinorUnits;
  creditMinor: MinorUnits;
}

export interface StatementSummary {
  loanId: number;
  openingBalanceMinor: MinorUnits;
  totalDebitsMinor: MinorUnits;
  totalCreditsMinor: MinorUnits;
  closingBalanceMinor: MinorUnits;
  lineCount: number;
}

export class ValidationError extends Error {
  constructor(readonly field: string, message: string) {
    super(message);
    this.name = 'ValidationError';
  }
}
