import { randomUUID } from 'crypto';
import { PaymentGatewayClient } from '../clients/payment-gateway-client';
import { LedgerSync } from '../ledger/ledger-sync';
import { ValidationError } from '../util/errors';
import { Logger } from '../util/logger';
import { DisbursementRecord, IdempotencyStore } from './idempotency-store';

export interface DisbursementCommand {
  idempotencyKey: string;
  loanId: string;
  destinationAccountId: string;
  amountMinor: number;
  currency: string;
}

/**
 * Pays out an approved loan. The portal retries the request on any timeout,
 * and the loan.approved consumer may deliver the same approval more than once,
 * so the same idempotency key can reach this service concurrently.
 */
export class DisbursementService {
  constructor(
    private readonly gateway: PaymentGatewayClient,
    private readonly store: IdempotencyStore,
    private readonly ledger: LedgerSync,
    private readonly log: Logger,
  ) {}

  async disburse(cmd: DisbursementCommand): Promise<DisbursementRecord> {
    this.validate(cmd);
    const existing = await this.store.find(cmd.idempotencyKey);
    if (existing) {
      this.log.info('disbursement replayed', { loanId: cmd.loanId, payoutId: existing.gatewayPayoutId });
      return existing;
    }
    const receipt = await this.gateway.submitPayout({
      payoutRef: randomUUID(),
      loanId: cmd.loanId,
      destinationAccountId: cmd.destinationAccountId,
      amountMinor: cmd.amountMinor,
      currency: cmd.currency,
    });
    const record: DisbursementRecord = {
      idempotencyKey: cmd.idempotencyKey,
      loanId: cmd.loanId,
      gatewayPayoutId: receipt.gatewayPayoutId,
      amountMinor: cmd.amountMinor,
      status: receipt.status,
      createdAt: new Date().toISOString(),
    };
    await this.store.save(cmd.idempotencyKey, record);
    if (receipt.status === 'ACCEPTED') {
      await this.ledger.post(cmd.loanId, 'DISBURSEMENT', cmd.amountMinor, `disb-${cmd.idempotencyKey}`);
    }
    this.log.info('disbursement submitted', { loanId: cmd.loanId, payoutId: receipt.gatewayPayoutId, status: receipt.status });
    return record;
  }

  private validate(cmd: DisbursementCommand): void {
    if (!cmd.idempotencyKey || cmd.idempotencyKey.length > 64 || !/^[A-Z]{3}$/.test(cmd.currency)) {
      throw new ValidationError(`disbursement for ${cmd.loanId} has an invalid key or currency`);
    }
    if (!Number.isInteger(cmd.amountMinor) || cmd.amountMinor <= 0) {
      throw new ValidationError(`disbursement for ${cmd.loanId} has a non-positive amount`);
    }
  }
}
