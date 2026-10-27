import { ChargeOffService } from '../collections/charge-off-service';
import { DelinquencyCollector, DelinquencySource, DelinquentPage } from '../collections/delinquency-collector';
import { DunningService } from '../collections/dunning-service';
import { Logger } from '../util/logger';

/**
 * Caps how much of a paged source is read. The nightly snapshot only ever
 * holds maxPages * pageSize rows; the remainder rolls to the next run, which
 * starts from the most overdue loans again.
 */
export async function* takePages(source: AsyncIterable<DelinquentPage>, maxPages: number): AsyncIterable<DelinquentPage> {
  let taken = 0;
  for await (const page of source) {
    yield page;
    if (++taken >= maxPages) {
      return;
    }
  }
}

export interface DelinquencyJobOptions {
  pageSize: number;
  maxPages: number;
}

export class DelinquencyJob {
  constructor(
    private readonly source: DelinquencySource,
    private readonly collector: DelinquencyCollector,
    private readonly dunning: DunningService,
    private readonly chargeOffs: ChargeOffService,
    private readonly options: DelinquencyJobOptions,
    private readonly log: Logger,
  ) {}

  async run(): Promise<void> {
    const pageSize = Math.min(this.options.pageSize, 1000);
    const pages = takePages(this.source.pages(pageSize), this.options.maxPages);
    const snapshot = await this.collector.collect(pages);
    this.log.info('delinquency snapshot built', {
      loans: snapshot.loans.length,
      totalOverdueMinor: snapshot.totalOverdueMinor,
    });

    await this.dunning.run(
      snapshot.loans.map((l) => ({ loanId: l.loanId, daysPastDue: l.daysPastDue, preferredChannel: 'EMAIL' as const })),
    );
    const chargeOffIds = snapshot.loans.filter((l) => l.daysPastDue >= 180).map((l) => l.loanId);
    await this.chargeOffs.chargeOffAll(chargeOffIds);
  }
}
