import { CurrencyCode, DisbursementRecord, DisbursementRequest, ValidationError } from '../domain/types';
import { GatewayClient } from '../http/gatewayClient';
import { KeyedMutex } from '../util/keyedMutex';
import { Logger } from '../util/logger';
import { DisbursementLedger } from './disbursementLedger';

const SUPPORTED_CURRENCIES: ReadonlySet<CurrencyCode> = new Set(['USD', 'EUR', 'GBP', 'INR']);
const ACCOUNT_REF_PATTERN = /^acct_[a-z0-9]{12,32}$/;
const MAX_SINGLE_PAYOUT_MINOR = 50_000_000;

/**
 * Validates an inbound disbursement request before anything is reserved.
 * Account references are opaque tokens issued by account-service; raw bank
 * details are never accepted here.
 */
export function validateDisbursementRequest(request: DisbursementRequest, now: Date): void {
  if (!request.requestId || request.requestId.length > 64) {
    throw new ValidationError('requestId', 'requestId is required and must be at most 64 characters');
  }
  if (!Number.isSafeInteger(request.loanId) || request.loanId <= 0) {
    throw new ValidationError('loanId', 'loanId must be a positive integer');
  }
  if (!ACCOUNT_REF_PATTERN.test(request.accountRef)) {
    throw new ValidationError('accountRef', 'accountRef must be an account-service token');
  }
  if (!SUPPORTED_CURRENCIES.has(request.currency)) {
    throw new ValidationError('currency', `unsupported currency ${String(request.currency)}`);
  }
  if (!Number.isSafeInteger(request.amountMinor) || request.amountMinor <= 0) {
    throw new ValidationError('amountMinor', 'amount must be a positive whole number of minor units');
  }
  if (request.amountMinor > MAX_SINGLE_PAYOUT_MINOR) {
    throw new ValidationError('amountMinor', 'amount exceeds the single-payout limit');
  }
  const requestedAt = Date.parse(request.requestedAt);
  if (Number.isNaN(requestedAt)) {
    throw new ValidationError('requestedAt', 'requestedAt must be an ISO-8601 timestamp');
  }
  if (requestedAt > now.getTime() + 5 * 60_000) {
    throw new ValidationError('requestedAt', 'requestedAt is too far in the future');
  }
}

/**
 * Orchestrates reserve -> submit -> record outcome for a loan payout.
 *
 * Every step that reads and then writes a loan's ledger state runs under the
 * per-loan lock, so two deliveries of a request for the same loan are handled
 * one after the other and the second one sees the first one's reservation.
 * Disbursement requests are partitioned by loan id upstream, so a given loan
 * is only ever processed by one worker process.
 */
export class DisbursementService {
  constructor(
    private readonly ledger: DisbursementLedger,
    private readonly gateway: GatewayClient,
    private readonly locks: KeyedMutex,
    private readonly logger: Logger,
    private readonly clock: () => Date = () => new Date(),
  ) {}

  async disburse(request: DisbursementRequest): Promise<DisbursementRecord> {
    validateDisbursementRequest(request, this.clock());

    return this.locks.runExclusive(`loan:${request.loanId}`, async () => {
      const { record, created } = await this.ledger.reserve(request);
      if (!created && record.status !== 'RESERVED') {
        this.logger.log('info', 'disbursement.replay', { loanId: record.loanId, disbursementId: record.disbursementId });
        return record;
      }

      try {
        const outcome = await this.gateway.submitPayout(record);
        if (outcome.kind === 'rejected') {
          this.logger.log('warn', 'disbursement.rejected', {
            disbursementId: record.disbursementId,
            reasonCode: outcome.reasonCode,
          });
          return this.ledger.markFailed(record.disbursementId);
        }
        return this.ledger.markSubmitted(record.disbursementId, outcome.gatewayReference);
      } catch (error) {
        this.logger.log('error', 'disbursement.submit_failed', {
          disbursementId: record.disbursementId,
          error: (error as Error).name,
        });
        throw error;
      }
    });
  }

  async cancel(loanId: number, disbursementId: string): Promise<DisbursementRecord> {
    return this.locks.runExclusive(`loan:${loanId}`, () => this.ledger.cancel(disbursementId));
  }
}
