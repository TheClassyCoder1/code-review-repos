import { FetchLike, readJson } from '../http/timed-fetch';
import { UpstreamError } from '../util/errors';

export interface BureauReport {
  borrowerRef: string;
  score: number;
  pulledAt: string;
  tradelines: Array<{ tradelineId: string; status: string; balanceMinor: number }>;
}

/**
 * Soft-pull client for the credit bureau. borrowerRef is our internal opaque
 * reference; the bureau maps it to the borrower on its side.
 */
export class BureauClient {
  constructor(
    private readonly baseUrl: string,
    private readonly fetchImpl: FetchLike,
  ) {}

  async softPull(borrowerRef: string): Promise<BureauReport> {
    const res = await this.fetchImpl(`${this.baseUrl}/v1/reports/${encodeURIComponent(borrowerRef)}?mode=soft`, {
      headers: { accept: 'application/json' },
    });
    if (!res.ok) {
      throw new UpstreamError(`bureau soft pull failed for ${borrowerRef}`, res.status);
    }
    return readJson<BureauReport>(res, 'bureau report');
  }
}
