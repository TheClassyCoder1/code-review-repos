import { buildDatabaseSettings, describeDatabase, DatabaseSettings } from './config/database.settings';
import { loadWebhookSigningKey, WebhookSigner } from './config/webhook-signing.config';
import { FeeCharge, IsoDate } from './domain/types';
import { LedgerRepository } from './repository/ledger.repository';
import { AmortizationService } from './services/amortization.service';
import { AchGateway, AutopayService } from './services/autopay.service';
import { CollectionsQueueService, Logger } from './services/collections-queue.service';
import { DelinquencyService } from './services/delinquency.service';
import { FeeStore, FeeWaiverService } from './services/fee-waiver.service';
import { PaymentAllocationService } from './services/payment-allocation.service';
import { PayoffQuoteService } from './services/payoff-quote.service';
import { StatementService } from './services/statement.service';
import { BusinessCalendar, daysBetween } from './util/business-calendar';

export interface ServicingModule {
  database: DatabaseSettings;
  calendar: BusinessCalendar;
  ledger: LedgerRepository;
  amortization: AmortizationService;
  allocation: PaymentAllocationService;
  delinquency: DelinquencyService;
  statements: StatementService;
  feeWaivers: FeeWaiverService;
  collections: CollectionsQueueService;
  payoff: PayoffQuoteService;
  autopay: AutopayService;
  webhookSigner: WebhookSigner;
}

export interface ServicingDependencies {
  holidays: Iterable<IsoDate>;
  achGateway: AchGateway;
  logger: Logger;
  fees?: FeeStore;
}

/** In-memory fee store used until fees move to the ledger database. */
export class InMemoryFeeStore implements FeeStore {
  private readonly fees = new Map<string, FeeCharge>();

  add(fee: FeeCharge): void {
    this.fees.set(fee.feeId, { ...fee });
  }

  find(feeId: string): FeeCharge | undefined {
    return this.fees.get(feeId);
  }

  markWaived(feeId: string): void {
    const fee = this.fees.get(feeId);
    if (fee) {
      fee.waived = true;
    }
  }

  waivedInLastYear(loanId: number, asOf: IsoDate): FeeCharge[] {
    return [...this.fees.values()].filter(
      (f) => f.loanId === loanId && f.waived && daysBetween(f.chargedOn, asOf) <= 365,
    );
  }
}

/** Composition root: every service receives its collaborators through its constructor. */
export function createServicingModule(deps: ServicingDependencies): ServicingModule {
  const database = buildDatabaseSettings();
  const calendar = new BusinessCalendar(deps.holidays);
  const ledger = new LedgerRepository();
  const webhookSigner = new WebhookSigner(loadWebhookSigningKey());

  deps.logger.info('servicing module starting', { database: describeDatabase(database) });

  return {
    database,
    calendar,
    ledger,
    amortization: new AmortizationService(calendar),
    allocation: new PaymentAllocationService(),
    delinquency: new DelinquencyService(),
    statements: new StatementService(ledger),
    feeWaivers: new FeeWaiverService(deps.fees ?? new InMemoryFeeStore(), ledger),
    collections: new CollectionsQueueService(deps.logger),
    payoff: new PayoffQuoteService(calendar),
    autopay: new AutopayService(deps.achGateway, ledger, calendar, deps.logger),
    webhookSigner,
  };
}
