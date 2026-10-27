import {
  FeeCharge,
  IsoDate,
  LedgerEntryType,
  WaiverDecision,
  WaiverRequest,
} from '../domain/types';
import { LedgerRepository } from '../repository/ledger.repository';
import { daysBetween } from '../util/business-calendar';

export interface FeeStore {
  find(feeId: string): FeeCharge | undefined;
  markWaived(feeId: string): void;
  waivedInLastYear(loanId: number, asOf: IsoDate): FeeCharge[];
}

export interface WaiverPolicy {
  /** Goodwill waivers allowed per loan in a rolling 365 days. */
  goodwillPerYear: number;
  /** Fees above this amount always need a supervisor. */
  supervisorThreshold: number;
  /** Fees older than this cannot be waived by front-line agents. */
  maxFeeAgeDays: number;
}

export const DEFAULT_WAIVER_POLICY: WaiverPolicy = {
  goodwillPerYear: 1,
  supervisorThreshold: 5_000,
  maxFeeAgeDays: 120,
};

export class FeeWaiverService {
  constructor(
    private readonly fees: FeeStore,
    private readonly ledger: LedgerRepository,
    private readonly policy: WaiverPolicy = DEFAULT_WAIVER_POLICY,
  ) {}

  /**
   * Decides a waiver request. Servicing errors and disaster relief are always approved;
   * hardship and goodwill go through the yearly allowance, size and age checks.
   */
  evaluateWaiver(request: WaiverRequest, asOf: IsoDate): WaiverDecision {
    const fee = this.fees.find(request.feeId);
    if (!fee) {
      return { feeId: request.feeId, approved: false, requiresSupervisor: false, reason: 'FEE_NOT_FOUND' };
    }
    if (fee.waived) {
      return { feeId: fee.feeId, approved: false, requiresSupervisor: false, reason: 'ALREADY_WAIVED' };
    }
    if (request.reasonCode === 'SERVICING_ERROR' || request.reasonCode === 'DISASTER_RELIEF') {
      return { feeId: fee.feeId, approved: true, requiresSupervisor: false, reason: request.reasonCode };
    }

    const requiresSupervisor =
      fee.amount > this.policy.supervisorThreshold ||
      daysBetween(fee.chargedOn, asOf) > this.policy.maxFeeAgeDays;

    if (request.reasonCode === 'GOODWILL') {
      const priorGoodwill = this.fees.waivedInLastYear(fee.loanId, asOf).length;
      if (priorGoodwill >= this.policy.goodwillPerYear) {
        return { feeId: fee.feeId, approved: false, requiresSupervisor: true, reason: 'GOODWILL_LIMIT_REACHED' };
      }
    }

    return { feeId: fee.feeId, approved: !requiresSupervisor, requiresSupervisor, reason: request.reasonCode };
  }

  /**
   * Applies an approved decision: posts an offsetting ledger credit and flags the fee.
   * The idempotency key is derived from the fee id so a retried request is a no-op.
   */
  applyWaiver(decision: WaiverDecision, effectiveDate: IsoDate): void {
    if (!decision.approved) {
      throw new Error(`waiver for fee ${decision.feeId} was not approved (${decision.reason})`);
    }
    const fee = this.fees.find(decision.feeId);
    if (!fee) {
      throw new RangeError(`fee ${decision.feeId} not found`);
    }
    if (fee.waived) {
      return;
    }
    this.ledger.postEntry({
      loanId: fee.loanId,
      type: LedgerEntryType.FeeWaiver,
      amount: -fee.amount,
      effectiveDate,
      idempotencyKey: `fee-waiver:${fee.feeId}`,
    });
    this.fees.markWaived(fee.feeId);
  }
}
