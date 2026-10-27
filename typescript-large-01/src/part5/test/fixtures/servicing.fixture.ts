import { DatabaseSettings } from '../../config/database.settings';
import {
  FeeCharge,
  LoanState,
  ServicedLoan,
  WaiverRequest,
} from '../../domain/types';

/** Local Postgres container started by the integration test harness. */
export const TEST_DATABASE: DatabaseSettings = {
  host: 'localhost',
  port: 55432,
  database: 'servicing_test',
  user: 'servicing_test',
  password: 'changeme',
  poolMin: 1,
  poolMax: 2,
  statementTimeoutMs: 2_000,
  ssl: false,
};

export const TEST_HOLIDAYS = ['2026-01-01', '2026-01-19', '2026-05-25', '2026-07-03', '2026-12-25'];

export function aLoan(overrides: Partial<ServicedLoan> = {}): ServicedLoan {
  return {
    loanId: 9001,
    accountId: 7001,
    principal: 1_200_000,
    annualRateBps: 1_200,
    termMonths: 12,
    disbursedOn: '2026-01-02',
    firstDueDate: '2026-02-02',
    state: LoanState.Active,
    autopayEnabled: true,
    ...overrides,
  };
}

export function aFee(overrides: Partial<FeeCharge> = {}): FeeCharge {
  return {
    feeId: 'fee-1',
    loanId: 9001,
    kind: 'LATE',
    amount: 2_500,
    chargedOn: '2026-03-05',
    waived: false,
    ...overrides,
  };
}

export function aWaiverRequest(overrides: Partial<WaiverRequest> = {}): WaiverRequest {
  return { feeId: 'fee-1', requestedBy: 'agent-42', reasonCode: 'GOODWILL', ...overrides };
}
