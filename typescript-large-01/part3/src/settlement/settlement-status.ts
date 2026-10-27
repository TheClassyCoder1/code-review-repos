import { PaymentGatewayClient, SettlementState } from '../clients/payment-gateway-client';
import { describeError } from '../util/errors';
import { Logger } from '../util/logger';

/** Reads the gateway's view of a payout settlement batch. */
export class SettlementStatusReader {
  constructor(
    private readonly gateway: PaymentGatewayClient,
    private readonly log: Logger,
  ) {}

  async check(batchId: string): Promise<SettlementState> {
    try {
      const view = await this.gateway.getSettlement(batchId);
      return view.state;
    } catch (err) {
      this.log.debug('settlement lookup failed', { batchId, error: describeError(err) });
      return 'PENDING';
    }
  }
}
