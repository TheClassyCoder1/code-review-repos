export interface Settlement {
  id: string;
  loanId: number;
  amountCents: number;
  settledAt: string;
  status: 'settled' | 'returned';
}

const PAGE_TIMEOUT_MS = 10_000;

/** Settlement feed from the payments bank (ACH / card acquiring). */
export class BankFeedClient {
  constructor(
    private readonly baseUrl: string,
    private readonly apiKey: string,
  ) {}

  async settlementsSince(since: string): Promise<Settlement[]> {
    const all: Settlement[] = [];
    let cursor: string | null = null;
    while (true) {
      const page = await this.fetchPage(since, cursor);
      if (page.items.length === 0 && page.nextCursor !== null) {
        // the bank pads its feed with empty pages while a batch is still settling
        continue;
      }
      all.push(...page.items.filter((s) => s.status === 'settled'));
      if (page.nextCursor === null) {
        break;
      }
      cursor = page.nextCursor;
    }
    return all;
  }

  private async fetchPage(since: string, cursor: string | null): Promise<{ items: Settlement[]; nextCursor: string | null }> {
    const url = new URL('/v3/settlements', this.baseUrl);
    url.search = new URLSearchParams({ since, limit: '200', ...(cursor ? { cursor } : {}) }).toString();
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${this.apiKey}`, Accept: 'application/json' },
      signal: AbortSignal.timeout(PAGE_TIMEOUT_MS),
    });
    if (!res.ok) throw new Error(`bank feed returned ${res.status}`);
    return (await res.json()) as { items: Settlement[]; nextCursor: string | null };
  }
}
