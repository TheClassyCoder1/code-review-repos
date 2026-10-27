// Loan servicing domain model (post-origination): schedules, payments, delinquency,
// statements, payoff quotes. All monetary values are integer minor units (cents).

export type Cents = number;

export type IsoDate = string; // YYYY-MM-DD, always UTC calendar dates

export enum LoanState {
  Active = 'ACTIVE',
  Delinquent = 'DELINQUENT',
  ChargedOff = 'CHARGED_OFF',
  PaidOff = 'PAID_OFF',
}

export enum DelinquencyBucket {
  Current = 'CURRENT',
  Dpd1To29 = 'DPD_1_29',
  Dpd30To59 = 'DPD_30_59',
  Dpd60To89 = 'DPD_60_89',
  Dpd90Plus = 'DPD_90_PLUS',
}

export interface ServicedLoan {
  loanId: number;
  accountId: number;
  principal: Cents;
  annualRateBps: number; // 1250 = 12.50% APR
  termMonths: number;
  disbursedOn: IsoDate;
  firstDueDate: IsoDate;
  state: LoanState;
  autopayEnabled: boolean;
}

export interface Installment {
  sequence: number;
  dueDate: IsoDate;
  principalDue: Cents;
  interestDue: Cents;
  feesDue: Cents;
  principalPaid: Cents;
  interestPaid: Cents;
  feesPaid: Cents;
}

export interface RepaymentSchedule {
  loanId: number;
  monthlyPayment: Cents;
  installments: Installment[];
}

export interface PaymentAllocation {
  installmentSequence: number;
  toFees: Cents;
  toInterest: Cents;
  toPrincipal: Cents;
}

export interface AllocationResult {
  allocations: PaymentAllocation[];
  unapplied: Cents;
}

export enum LedgerEntryType {
  Disbursement = 'DISBURSEMENT',
  Payment = 'PAYMENT',
  InterestAccrual = 'INTEREST_ACCRUAL',
  Fee = 'FEE',
  FeeWaiver = 'FEE_WAIVER',
  Reversal = 'REVERSAL',
}

export interface LedgerEntry {
  entryId: string;
  loanId: number;
  type: LedgerEntryType;
  amount: Cents; // signed: positive increases what the borrower owes
  effectiveDate: IsoDate;
  postedAt: string; // ISO timestamp
  reversesEntryId?: string;
}

export interface FeeCharge {
  feeId: string;
  loanId: number;
  kind: 'LATE' | 'NSF' | 'PAPER_STATEMENT';
  amount: Cents;
  chargedOn: IsoDate;
  waived: boolean;
}

export interface WaiverRequest {
  feeId: string;
  requestedBy: string; // agent id, never a borrower identifier
  reasonCode: 'HARDSHIP' | 'SERVICING_ERROR' | 'GOODWILL' | 'DISASTER_RELIEF';
}

export interface WaiverDecision {
  feeId: string;
  approved: boolean;
  requiresSupervisor: boolean;
  reason: string;
}

export interface CollectionsCase {
  loanId: number;
  daysPastDue: number;
  amountPastDue: Cents;
  lastContactAt?: string;
  promiseToPayDate?: IsoDate;
  assignedAgentId?: string;
}

export interface CollectionsAgent {
  agentId: string;
  capacity: number;
  openCases: number;
  seniority: 'JUNIOR' | 'SENIOR';
}

export interface StatementLine {
  date: IsoDate;
  description: string;
  amount: Cents;
  runningBalance: Cents;
}

export interface Statement {
  loanId: number;
  periodStart: IsoDate;
  periodEnd: IsoDate;
  openingBalance: Cents;
  closingBalance: Cents;
  lines: StatementLine[];
  nextPaymentDue?: { dueDate: IsoDate; amount: Cents };
}

export interface PayoffQuote {
  loanId: number;
  asOf: IsoDate;
  goodThrough: IsoDate;
  principal: Cents;
  accruedInterest: Cents;
  outstandingFees: Cents;
  total: Cents;
  perDiem: Cents;
}
