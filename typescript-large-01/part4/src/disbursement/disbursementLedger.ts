import { randomUUID } from 'crypto';
import { applyTransition } from '../domain/statusMachine';
import { DisbursementRecord, DisbursementRequest } from '../domain/types';
import { DisbursementRepository } from './ledgerRepository';

export interface ReservationResult {
  record: DisbursementRecord;
  created: boolean;
}

/**
 * Tracks the single open disbursement a loan may have at any time. A loan is
 * paid out once; a second request for the same loan while one is open returns
 * the existing reservation instead of creating another.
 */
export class DisbursementLedger {
  constructor(
    private readonly repository: DisbursementRepository,
    private readonly clock: () => Date = () => new Date(),
  ) {}

  async reserve(request: DisbursementRequest): Promise<ReservationResult> {
    const existing = await this.repository.findOpenByLoan(request.loanId);
    if (existing) {
      return { record: existing, created: false };
    }

    const now = this.clock().toISOString();
    const requested: DisbursementRecord = {
      disbursementId: randomUUID(),
      loanId: request.loanId,
      accountRef: request.accountRef,
      amountMinor: request.amountMinor,
      currency: request.currency,
      status: 'REQUESTED',
      gatewayReference: null,
      createdAt: now,
      updatedAt: now,
    };
    const reserved = applyTransition(requested, 'RESERVED', now);
    await this.repository.insert(reserved);
    return { record: reserved, created: true };
  }

  async markSubmitted(disbursementId: string, gatewayReference: string): Promise<DisbursementRecord> {
    return this.advance(disbursementId, (record, now) => applyTransition(record, 'SUBMITTED', now, gatewayReference));
  }

  async markFailed(disbursementId: string): Promise<DisbursementRecord> {
    return this.advance(disbursementId, (record, now) => applyTransition(record, 'FAILED', now));
  }

  async cancel(disbursementId: string): Promise<DisbursementRecord> {
    return this.advance(disbursementId, (record, now) => applyTransition(record, 'CANCELLED', now));
  }

  private async advance(
    disbursementId: string,
    change: (record: DisbursementRecord, nowIso: string) => DisbursementRecord,
  ): Promise<DisbursementRecord> {
    const current = await this.repository.findById(disbursementId);
    if (!current) {
      throw new Error(`unknown disbursement ${disbursementId}`);
    }
    const next = change(current, this.clock().toISOString());
    if (next !== current) {
      await this.repository.update(next);
    }
    return next;
  }
}
