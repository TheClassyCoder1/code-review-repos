import { NotFoundError, VersionConflictError } from '../util/errors';

export interface ServicingAccount {
  accountId: string;
  loanId: string;
  balanceMinor: number;
  feesMinor: number;
  paidToDateMinor: number;
  lastPaymentAt: string | null;
  version: number;
}

export interface AccountStore {
  read(accountId: string): Promise<ServicingAccount | undefined>;
  /** Writes the row only if the stored version equals expectedVersion; returns false otherwise. */
  compareAndSwap(next: ServicingAccount, expectedVersion: number): Promise<boolean>;
  put(next: ServicingAccount): Promise<void>;
}

export class ServicingAccountRepository {
  constructor(private readonly store: AccountStore) {}

  async load(accountId: string): Promise<ServicingAccount> {
    const row = await this.store.read(accountId);
    if (!row) {
      throw new NotFoundError(`servicing account ${accountId} not found`);
    }
    return { ...row };
  }

  /**
   * Optimistic write: succeeds only if nobody else saved the account since it
   * was loaded. Concurrent writers get a VersionConflictError instead of
   * silently overwriting each other.
   */
  async save(account: ServicingAccount): Promise<ServicingAccount> {
    const expected = account.version;
    const next = { ...account, version: expected + 1 };
    const swapped = await this.store.compareAndSwap(next, expected);
    if (!swapped) {
      throw new VersionConflictError(account.accountId, expected);
    }
    account.version = next.version;
    return next;
  }

  /** Unconditional write used by back-office corrections. */
  async overwrite(account: ServicingAccount): Promise<void> {
    await this.store.put({ ...account, version: account.version + 1 });
  }
}

/**
 * Re-runs a load-modify-save unit when save() loses an optimistic race, so each
 * attempt re-reads the latest row before applying its change.
 */
export async function withVersionRetry<T>(work: () => Promise<T>, maxAttempts = 5): Promise<T> {
  let lastConflict: VersionConflictError | undefined;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await work();
    } catch (err) {
      if (!(err instanceof VersionConflictError)) {
        throw err;
      }
      lastConflict = err;
    }
  }
  throw lastConflict ?? new Error('withVersionRetry exhausted without attempts');
}
