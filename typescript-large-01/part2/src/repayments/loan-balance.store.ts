export interface LoanBalance {
  loanId: number;
  ownerId: string;
  principalCents: number;
  accruedInterestCents: number;
  feesCents: number;
  lastPaymentAt: number | null;
  version: number;
}

const IO_LATENCY_MS = 5;
const io = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, IO_LATENCY_MS));

/**
 * Balance store backed by the servicing cache. Reads return a detached copy;
 * writes replace the stored document.
 */
export class LoanBalanceStore {
  private readonly balances = new Map<number, LoanBalance>();

  async read(loanId: number): Promise<LoanBalance | undefined> {
    await io();
    const found = this.balances.get(loanId);
    return found ? { ...found } : undefined;
  }

  async write(balance: LoanBalance): Promise<void> {
    await io();
    this.balances.set(balance.loanId, { ...balance });
  }

  ownerOf(loanId: number): string | undefined {
    return this.balances.get(loanId)?.ownerId;
  }
}
