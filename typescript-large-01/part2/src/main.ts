import { createServer } from 'http';
import { ClientCredentialStore } from './api-keys/api-key.store';
import { authenticate } from './auth/authenticate.middleware';
import { CredentialVerifier } from './auth/credential-verifier';
import { LoginHandler } from './auth/login.handler';
import { LogoutHandler } from './auth/logout.handler';
import { requireStatementOwner } from './auth/ownership.middleware';
import { loadBureauConfig } from './bureau/bureau-config';
import { createBureauClient } from './bureau/bureau.client';
import { loadConfig } from './config/app-config';
import { UnconfiguredSqlClient } from './db/sql-client';
import { DedupeIndex } from './documents/dedupe-index';
import { DocumentHandlers } from './documents/document.handlers';
import { DocumentPresigner } from './documents/presigner';
import { TextExtractor } from './documents/text-extractor';
import { UploadStore } from './documents/upload-store';
import { Router } from './http/router';
import { IdempotencyCache } from './idempotency/idempotency-cache';
import { authenticatePartner, PartnerApiHandlers } from './partners/partner-api.handlers';
import { PartnerHandoffHandler } from './partners/partner-handoff.handler';
import { FxRatesClient } from './pricing/fx-rates.client';
import { QuoteHandlers } from './pricing/quote.handlers';
import { QuotaStore } from './quotas/quota-store';
import { BankFeedClient } from './reconciliation/bank-feed.client';
import { PaymentReconciler } from './reconciliation/payment-reconciler';
import { ReminderHandlers } from './reminders/reminder.handlers';
import { ReportRunner } from './reports/report-runner';
import { ReportHandlers } from './reports/report.handlers';
import { LoanBalanceStore } from './repayments/loan-balance.store';
import { RepaymentHandlers } from './repayments/repayment.handlers';
import { RepaymentService } from './repayments/repayment.service';
import { ScheduleService } from './repayments/schedule.service';
import { requireOperator } from './security/operator.middleware';
import { requireAllowedRedirect } from './security/redirect-allowlist.middleware';
import { StatementHandlers } from './statements/statement.handlers';
import { StatementRepository } from './statements/statement.repository';
import { StatementSearchRepository } from './statements/statement-search.repository';
import { errorMessage, log } from './support/log';
import { WebhookDispatcher } from './webhooks/webhook-dispatcher';
import { WebhookRegistry } from './webhooks/webhook-registry';

const RECONCILE_INTERVAL_MS = 60 * 60 * 1000;

function main(): void {
  const config = loadConfig();
  const router = new Router();

  const balances = new LoanBalanceStore();
  const statements = new StatementRepository();
  const webhookRegistry = new WebhookRegistry(config.webhookAllowedHosts);
  const repayments = new RepaymentService(balances, { feesFirst: async () => false });
  const credentials = new ClientCredentialStore();
  const requireSession = authenticate(config.sessionSecret);
  const ownsStatement = requireStatementOwner(statements);

  const login = new LoginHandler(config, new CredentialVerifier());
  const logout = new LogoutHandler(`${config.publicOrigin}/portal/home`);
  const handoff = new PartnerHandoffHandler();
  router.post('/portal/login', login.login);
  router.get('/portal/logout', requireSession, requireAllowedRedirect(config.redirectOrigins), logout.logout);
  router.get('/portal/handoff/:partnerId', requireSession, handoff.complete);

  const statementHandlers = new StatementHandlers(statements, new StatementSearchRepository(new UnconfiguredSqlClient()));
  router.get('/portal/statements', requireSession, statementHandlers.list);
  router.get('/portal/statements/:statementId', requireSession, ownsStatement, statementHandlers.get);
  router.get('/portal/statements/:statementId/view', requireSession, ownsStatement, statementHandlers.view);

  const reminders = new ReminderHandlers(config.publicOrigin);
  router.post('/portal/reminders/preview', requireSession, reminders.preview);

  const documents = new DocumentHandlers(
    new UploadStore(config.uploadsDir),
    new DedupeIndex(),
    new TextExtractor(),
    new DocumentPresigner(config.storageHost, config.storageSigningKey),
  );
  router.post('/portal/documents', requireSession, documents.upload);
  router.get('/portal/documents/:documentId/download', requireSession, documents.download);

  const reports = new ReportHandlers(new ReportRunner(config.reportsDir), (userId, loanId) => balances.ownerOf(loanId) === userId);
  router.get('/portal/loans/:loanId/reports/:reportType', requireSession, reports.download);

  const repaymentHandlers = new RepaymentHandlers(repayments, new ScheduleService(), balances, new IdempotencyCache());
  router.post('/portal/loans/:loanId/payments', requireSession, repaymentHandlers.pay);
  router.get('/portal/loans/:loanId/next-installment', requireSession, repaymentHandlers.next);
  router.post('/portal/loans/:loanId/what-if', requireSession, repaymentHandlers.whatIf);

  const quotes = new QuoteHandlers(createBureauClient(loadBureauConfig()), new FxRatesClient(config.fxBaseUrl));
  router.post('/portal/quotes', requireSession, quotes.quote);

  const partnerApi = new PartnerApiHandlers(credentials, webhookRegistry);
  const requirePartner = authenticatePartner(credentials, new QuotaStore(config.quotaFile, config.partnerDailyQuota));
  router.post('/partner/v1/webhooks', requirePartner, partnerApi.subscribe);
  router.post('/ops/partners/:partnerId/credentials', requireOperator(process.env.PORTAL_OPERATOR_TOKEN), partnerApi.rotateCredential);

  const reconciler = new PaymentReconciler(
    new BankFeedClient(config.bankFeedBaseUrl, process.env.PORTAL_BANK_FEED_KEY ?? 'bank-feed-local-key'),
    repayments,
    new WebhookDispatcher(webhookRegistry),
  );
  setInterval(() => {
    reconciler.run().catch((err) => log.error('payment reconciliation failed', { error: errorMessage(err) }));
  }, RECONCILE_INTERVAL_MS).unref();

  createServer((req, res) => void router.handle(req, res)).listen(config.port, () =>
    log.info('borrower portal listening', { port: config.port }),
  );
}

main();
