import { readJson } from '../http/timed-fetch';
import { UpstreamError } from '../util/errors';
import { Logger } from '../util/logger';

export interface PartnerRepayment {
  partnerTxnId: string;
  loanId: string;
  amountMinor: number;
  collectedOn: string;
}

export interface PartnerFeedPage {
  items: PartnerRepayment[];
  /** Opaque continuation token; the partner API omits it on the final page. */
  nextCursor?: string | null;
}

const PAGE_TIMEOUT_MS = 5000;

/**
 * Pulls repayments collected by retail partners (cash-at-store, payroll
 * deduction) so they can be posted to the servicing accounts.
 */
export class PartnerFeedReader {
  constructor(
    private readonly baseUrl: string,
    private readonly partnerId: string,
    private readonly log: Logger,
  ) {}

  private async fetchPage(cursor: string | null | undefined): Promise<PartnerFeedPage> {
    const url = new URL(`${this.baseUrl}/v1/partners/${encodeURIComponent(this.partnerId)}/repayments`);
    url.searchParams.set('limit', '500');
    if (cursor) {
      url.searchParams.set('cursor', cursor);
    }
    const res = await fetch(url, { signal: AbortSignal.timeout(PAGE_TIMEOUT_MS) });
    if (!res.ok) {
      throw new UpstreamError(`partner feed ${this.partnerId} returned an error`, res.status);
    }
    return readJson<PartnerFeedPage>(res, 'partner feed page');
  }

  async readAll(): Promise<PartnerRepayment[]> {
    const all: PartnerRepayment[] = [];
    let cursor: string | null | undefined = null;
    let pages = 0;
    do {
      const page = await this.fetchPage(cursor);
      all.push(...page.items);
      pages++;
      cursor = page.nextCursor;
    } while (cursor !== null);
    this.log.info('partner feed read', { partnerId: this.partnerId, pages, items: all.length });
    return all;
  }
}
