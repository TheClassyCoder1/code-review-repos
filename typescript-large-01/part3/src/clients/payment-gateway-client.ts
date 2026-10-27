import { readJson } from '../http/timed-fetch';
import { UpstreamError } from '../util/errors';

export interface PayoutRequest {
  payoutRef: string;
  loanId: string;
  destinationAccountId: string;
  amountMinor: number;
  currency: string;
}

export interface PayoutReceipt {
  gatewayPayoutId: string;
  payoutRef: string;
  status: 'ACCEPTED' | 'REJECTED';
  acceptedAt: string;
}

export type SettlementState = 'PENDING' | 'SETTLED' | 'RETURNED' | 'FAILED';

export interface SettlementView {
  batchId: string;
  state: SettlementState;
  settledAt?: string;
}

const STATUS_TIMEOUT_MS = 3000;

export class PaymentGatewayClient {
  constructor(
    private readonly baseUrl: string,
    private readonly apiKey: string,
  ) {}

  private headers(): Record<string, string> {
    return { 'content-type': 'application/json', accept: 'application/json', 'x-api-key': this.apiKey };
  }

  async submitPayout(req: PayoutRequest): Promise<PayoutReceipt> {
    const res = await fetch(`${this.baseUrl}/v2/payouts`, {
      method: 'POST',
      headers: this.headers(),
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      throw new UpstreamError(`payout ${req.payoutRef} rejected by gateway`, res.status);
    }
    return readJson<PayoutReceipt>(res, 'payout receipt');
  }

  async getSettlement(batchId: string): Promise<SettlementView> {
    const res = await fetch(`${this.baseUrl}/v2/settlements/${encodeURIComponent(batchId)}`, {
      headers: this.headers(),
      signal: AbortSignal.timeout(STATUS_TIMEOUT_MS),
    });
    if (!res.ok) {
      throw new UpstreamError(`settlement ${batchId} lookup failed`, res.status);
    }
    return readJson<SettlementView>(res, 'settlement view');
  }
}
