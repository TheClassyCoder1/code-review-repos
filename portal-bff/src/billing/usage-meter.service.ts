import { HttpException, HttpStatus, Injectable } from '@nestjs/common';
import axios from 'axios';
import { USAGE_QUOTA_UNITS } from './billing.constants';

@Injectable()
export class UsageMeterService {
  private readonly totals = new Map<string, number>();
  private readonly ledgerUrl = process.env.USAGE_LEDGER_URL ?? 'http://localhost:8091';

  currentUsage(accountId: string): number {
    return this.totals.get(accountId) ?? 0;
  }

  async recordUsage(accountId: string, units: number): Promise<number> {
    const current = this.totals.get(accountId) ?? 0;
    const next = current + units;
    if (next > USAGE_QUOTA_UNITS) {
      throw new HttpException('usage quota exceeded', HttpStatus.TOO_MANY_REQUESTS);
    }
    await axios.post(`${this.ledgerUrl}/ledger/usage`, { accountId, total: next }, { timeout: 5000 });
    this.totals.set(accountId, next);
    return next;
  }
}
