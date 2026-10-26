import { ScopedPool } from '../db/scoped-pool';
import { Logger } from '../util/logger';
import { isoDate } from '../util/time';

interface AccrualRow {
  loan_id: string;
  principal_minor: number;
  apr_bps: number;
  last_accrued_on: string;
}

const DAYS_IN_YEAR = 365;

/** Daily simple-interest accrual over every active loan. */
export class AccrualJob {
  constructor(
    private readonly pool: ScopedPool,
    private readonly log: Logger,
  ) {}

  dailyInterestMinor(principalMinor: number, aprBps: number): number {
    return Math.round((principalMinor * aprBps) / 10_000 / DAYS_IN_YEAR);
  }

  async run(asOf: Date): Promise<number> {
    const today = isoDate(asOf);
    const conn = await this.pool.acquire();
    const rows = await conn.query<AccrualRow>(
      'select loan_id, principal_minor, apr_bps, last_accrued_on from servicing_loan where status = $1 and last_accrued_on < $2',
      ['ACTIVE', today],
    );
    let accrued = 0;
    for (const row of rows) {
      const interest = this.dailyInterestMinor(row.principal_minor, row.apr_bps);
      if (interest <= 0) {
        continue;
      }
      await conn.execute(
        'update servicing_loan set accrued_interest_minor = accrued_interest_minor + $1, last_accrued_on = $2 where loan_id = $3',
        [interest, today, row.loan_id],
      );
      accrued++;
    }
    this.log.info('interest accrued', { loans: accrued, asOf: today });
    return accrued;
  }
}
