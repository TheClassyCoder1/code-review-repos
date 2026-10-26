import { FeeAssessmentService, FeePolicy } from './accounts/fee-assessment-service';
import { PaymentPostingService } from './accounts/payment-posting-service';
import { AccountStore, ServicingAccountRepository } from './accounts/servicing-account-repository';
import { BureauClient } from './clients/bureau-client';
import { LedgerClient } from './clients/ledger-client';
import { PaymentGatewayClient } from './clients/payment-gateway-client';
import { ChargeOffService, LoanStatusStore } from './collections/charge-off-service';
import { DelinquencyCollector, DelinquencySource } from './collections/delinquency-collector';
import { DunningService } from './collections/dunning-service';
import { ServicingConfig } from './config/servicing-config';
import { ConnectionFactory, ScopedPool } from './db/scoped-pool';
import { DisbursementService } from './disbursement/disbursement-service';
import { IdempotencyStore } from './disbursement/idempotency-store';
import { timedFetch } from './http/timed-fetch';
import { AccrualJob } from './jobs/accrual-job';
import { DelinquencyJob } from './jobs/delinquency-job';
import { LedgerSync } from './ledger/ledger-sync';
import { FileLock } from './locks/file-lock';
import { NoticeOutbox, NoticeTransport, OutboxRelay, OutboxStore } from './notifications/outbox';
import { RATE_TABLE } from './pricing/quote-request';
import { RateQuote, RateQuoteService } from './pricing/rate-quote-service';
import { DeadLetterQueue, DeadLetterSink } from './reconciliation/dead-letter';
import { EntryMatcher } from './reconciliation/entry-matcher';
import { BankStatementSource, DailyReconciler } from './reconciliation/reconciler';
import { SettlementAlerts, SettlementPoller } from './settlement/settlement-poller';
import { SettlementStatusReader } from './settlement/settlement-status';
import { StatementJob, StatementLineSource } from './statements/statement-job';
import { BoundedMap } from './util/bounded-map';
import { createLogger } from './util/logger';

/** Storage and integration adapters supplied by the hosting process. */
export interface ServicingPorts {
  accounts: AccountStore;
  feePolicy: FeePolicy;
  idempotency: IdempotencyStore;
  loanStatus: LoanStatusStore;
  delinquency: DelinquencySource;
  outbox: OutboxStore;
  noticeTransport: NoticeTransport;
  deadLetters: DeadLetterSink;
  matcher: EntryMatcher;
  bank: BankStatementSource;
  settlementAlerts: SettlementAlerts;
  statementLines: StatementLineSource;
  connections: ConnectionFactory;
}

export function createServicingModule(cfg: ServicingConfig, ports: ServicingPorts) {
  const log = (name: string) => createLogger(`servicing.${name}`);

  const ledger = new LedgerSync(new LedgerClient(cfg.ledgerBaseUrl), cfg.ledgerTimeoutMs, log('ledger'));
  const gateway = new PaymentGatewayClient(cfg.gatewayBaseUrl, cfg.gatewayApiKey);
  const accounts = new ServicingAccountRepository(ports.accounts);

  const outbox = new NoticeOutbox(ports.outbox, ports.noticeTransport);
  const relay = new OutboxRelay(outbox, ports.outbox, log('outbox-relay'));
  relay.start(15_000);

  const dunning = new DunningService(outbox, log('dunning'));
  const chargeOffs = new ChargeOffService(ledger, ports.loanStatus, log('charge-off'));
  const dlq = new DeadLetterQueue(ports.deadLetters, log('dead-letter'));

  const pool = new ScopedPool(ports.connections, cfg.dbPoolSize);
  const accrual = new AccrualJob(pool, log('accrual'));
  const delinquencyJob = new DelinquencyJob(
    ports.delinquency,
    new DelinquencyCollector(),
    dunning,
    chargeOffs,
    { pageSize: cfg.delinquentPageSize, maxPages: cfg.maxDelinquentPages },
    log('delinquency'),
  );
  const locks = new FileLock(cfg.lockDir);
  const statements = new StatementJob(ports.statementLines, cfg.exportDir, log('statements'));

  return {
    bureau: new BureauClient(cfg.bureauBaseUrl, timedFetch(cfg.bureauTimeoutMs)),
    payments: new PaymentPostingService(accounts, ledger, log('payments')),
    fees: new FeeAssessmentService(accounts, ports.feePolicy, ledger, log('fees')),
    disbursements: new DisbursementService(gateway, ports.idempotency, ledger, log('disbursement')),
    reconciler: new DailyReconciler(ports.bank, dlq.protect('bank-reconciliation', (e) => ports.matcher.match(e)), log('reconciler')),
    settlements: new SettlementPoller(
      new SettlementStatusReader(gateway, log('settlement')),
      ports.settlementAlerts,
      cfg.settlementMaxPolls,
      cfg.settlementPollIntervalMs,
      log('settlement'),
    ),
    quotes: new RateQuoteService(RATE_TABLE, new BoundedMap<string, RateQuote>(cfg.quoteCacheSize), log('quotes')),

    async runNightly(asOf: Date): Promise<void> {
      await locks.runExclusive('nightly-accrual', () => pool.runScope('accrual', () => accrual.run(asOf)));
      await locks.runExclusive('nightly-delinquency', () => delinquencyJob.run());
    },

    async exportStatements(accountIds: string[], period: string): Promise<void> {
      await locks.runExclusive(`statements-${period}`, () => statements.exportAll(accountIds, period));
    },

    shutdown(): void {
      relay.stop();
    },
  };
}
