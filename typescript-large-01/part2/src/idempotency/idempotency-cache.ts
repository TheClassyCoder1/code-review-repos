export interface CachedResponse {
  status: number;
  body: unknown;
  storedAt: number;
}

/**
 * Remembers the response for each Idempotency-Key so a borrower who taps
 * "Pay" twice (or whose app retries after a dropped connection) is not charged
 * twice. Keys are scoped per borrower by the caller.
 */
export class IdempotencyCache {
  private readonly entries = new Map<string, CachedResponse>();

  lookup(key: string): CachedResponse | undefined {
    return this.entries.get(key);
  }

  store(key: string, status: number, body: unknown): void {
    this.entries.set(key, { status, body, storedAt: Date.now() });
  }
}
