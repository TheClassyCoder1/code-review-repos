import { LedgerSync } from '../ledger/ledger-sync';
import { ValidationError } from '../util/errors';
import { Logger } from '../util/logger';
import { ServicingAccount, ServicingAccountRepository, withVersionRetry } from './servicing-account-repository';

export interface IncomingPayment {
  paymentId: string;
  accountId: string;
  amountMinor: number;
  receivedAt: Date;
}

/**
 * Applies a borrower payment: outstanding fees first, then principal.
 * Payments for one account can arrive concurrently from the card processor
 * and the bank-transfer webhook.
 */
export class PaymentPostingService {
  constructor(
    private readonly accounts: ServicingAccountRepository,
    private readonly ledger: LedgerSync,
    private readonly log: Logger,
  ) {}

  async post(payment: IncomingPayment): Promise<ServicingAccount> {
    if (!Number.isInteger(payment.amountMinor) || payment.amountMinor <= 0) {
      throw new ValidationError(`payment ${payment.paymentId} has a non-positive amount`);
    }
    const updated = await withVersionRetry(() => this.applyOnce(payment));
    await this.ledger.post(updated.loanId, 'REPAYMENT', payment.amountMinor, `pay-${payment.paymentId}`, payment.receivedAt);
    this.log.info('payment posted', { paymentId: payment.paymentId, accountId: payment.accountId });
    return updated;
  }

  private async applyOnce(payment: IncomingPayment): Promise<ServicingAccount> {
    const account = await this.accounts.load(payment.accountId);
    const toFees = Math.min(account.feesMinor, payment.amountMinor);
    const toPrincipal = Math.min(account.balanceMinor, payment.amountMinor - toFees);
    account.feesMinor -= toFees;
    account.balanceMinor -= toPrincipal;
    account.paidToDateMinor += payment.amountMinor;
    account.lastPaymentAt = payment.receivedAt.toISOString();
    return this.accounts.save(account);
  }
}
