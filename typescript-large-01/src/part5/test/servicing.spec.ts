import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildDatabaseSettings } from '../config/database.settings';
import { PlaceholderSecretError, MissingSecretError } from '../config/secret-guard';
import { loadWebhookSigningKey, WebhookSigner } from '../config/webhook-signing.config';
import { DelinquencyBucket, LedgerEntryType } from '../domain/types';
import { LedgerRepository } from '../repository/ledger.repository';
import { AmortizationService } from '../services/amortization.service';
import { CollectionsQueueService } from '../services/collections-queue.service';
import { DelinquencyService } from '../services/delinquency.service';
import { FeeStore, FeeWaiverService } from '../services/fee-waiver.service';
import { PaymentAllocationService } from '../services/payment-allocation.service';
import { PayoffQuoteService } from '../services/payoff-quote.service';
import { StatementService } from '../services/statement.service';
import { BusinessCalendar } from '../util/business-calendar';
import { roundHalfEven, splitEvenly } from '../util/money';
import { withRetry, HttpStatusError, isTransientHttpError } from '../util/retry';
import { TEST_DATABASE, TEST_HOLIDAYS, aFee, aLoan, aWaiverRequest } from './fixtures/servicing.fixture';

const calendar = new BusinessCalendar(TEST_HOLIDAYS);
const silentLogger = { info: () => undefined, warn: () => undefined };

test('half-even rounding and even splits', () => {
  assert.equal(roundHalfEven(2.5), 2);
  assert.equal(roundHalfEven(3.5), 4);
  assert.equal(roundHalfEven(3.51), 4);
  assert.deepEqual(splitEvenly(100, 3), [34, 33, 33]);
  assert.deepEqual(splitEvenly(-100, 3), [-34, -33, -33]);
});

test('schedule principal sums to the loan amount', () => {
  const svc = new AmortizationService(calendar);
  const loan = aLoan();
  const schedule = svc.buildSchedule(loan);
  assert.equal(schedule.installments.length, 12);
  const principal = schedule.installments.reduce((acc, i) => acc + i.principalDue, 0);
  assert.equal(principal, loan.principal);
  assert.equal(svc.computeMonthlyPayment(1_200_000, 0, 12), 100_000);
});

test('due dates clamp to month end without drifting', () => {
  assert.equal(calendar.nthDueDate('2026-01-31', 2), '2026-02-28');
  assert.equal(calendar.nthDueDate('2026-01-31', 3), '2026-03-31');
  assert.equal(calendar.nthDueDate('2026-11-15', 3), '2027-01-15');
  assert.equal(calendar.addBusinessDays('2026-07-02', 1), '2026-07-06');
});

test('allocation follows fees, interest, principal, oldest first', () => {
  const schedule = new AmortizationService(calendar).buildSchedule(aLoan());
  schedule.installments[0].feesDue = 2_500;
  const svc = new PaymentAllocationService();
  const first = schedule.installments[0];
  const result = svc.allocate(schedule, 2_500 + first.interestDue + 1);
  assert.deepEqual(result.allocations, [
    { installmentSequence: 1, toFees: 2_500, toInterest: first.interestDue, toPrincipal: 1 },
  ]);
  assert.equal(result.unapplied, 0);
  const applied = svc.apply(schedule, result);
  assert.equal(applied.installments[0].principalPaid, 1);
  assert.equal(schedule.installments[0].principalPaid, 0);
});

test('delinquency buckets and portfolio rate', () => {
  const svc = new DelinquencyService();
  const schedule = new AmortizationService(calendar).buildSchedule(aLoan());
  const { days } = svc.computeDaysPastDue(schedule, '2026-03-10');
  assert.equal(days, 36);
  assert.equal(svc.bucketFor(days), DelinquencyBucket.Dpd30To59);
  const summary = svc.summarizePortfolio([svc.snapshot(aLoan(), schedule, '2026-03-10')], '2026-03-10');
  assert.equal(summary.delinquencyRate, 1);
});

test('ledger replays are idempotent and reversals are single-use', () => {
  const ledger = new LedgerRepository(() => new Date('2026-02-01T00:00:00Z'));
  const fee = ledger.postEntry({
    loanId: 1, type: LedgerEntryType.Fee, amount: 2_500, effectiveDate: '2026-02-01', idempotencyKey: 'k1',
  });
  assert.equal(ledger.postEntry({
    loanId: 1, type: LedgerEntryType.Fee, amount: 2_500, effectiveDate: '2026-02-01', idempotencyKey: 'k1',
  }), fee);
  ledger.postEntry({
    loanId: 1, type: LedgerEntryType.Reversal, amount: -2_500, effectiveDate: '2026-02-02',
    reversesEntryId: fee.entryId, idempotencyKey: 'k2',
  });
  assert.equal(ledger.balanceAsOf(1, '2026-02-01'), 2_500);
  assert.equal(ledger.balanceAsOf(1, '2026-02-02'), 0);
  const statement = new StatementService(ledger).buildStatement(
    new AmortizationService(calendar).buildSchedule(aLoan({ loanId: 1 })), '2026-02-01', '2026-02-28');
  assert.equal(statement.closingBalance, 0);
});

test('goodwill waiver is limited per year', () => {
  const fee = aFee();
  const store: FeeStore = {
    find: (id) => (id === fee.feeId ? fee : undefined),
    markWaived: () => { fee.waived = true; },
    waivedInLastYear: () => [aFee({ feeId: 'fee-0', waived: true })],
  };
  const svc = new FeeWaiverService(store, new LedgerRepository());
  const decision = svc.evaluateWaiver(aWaiverRequest(), '2026-03-10');
  assert.equal(decision.approved, false);
  assert.equal(decision.reason, 'GOODWILL_LIMIT_REACHED');
});

test('collections queue holds back promised and recently contacted cases', () => {
  const svc = new CollectionsQueueService(silentLogger, () => new Date('2026-03-10T12:00:00Z'));
  const queue = svc.prioritize([
    { loanId: 1, daysPastDue: 40, amountPastDue: 10_000 },
    { loanId: 2, daysPastDue: 70, amountPastDue: 5_000 },
    { loanId: 3, daysPastDue: 90, amountPastDue: 5_000, promiseToPayDate: '2026-03-12' },
    { loanId: 4, daysPastDue: 95, amountPastDue: 5_000, lastContactAt: '2026-03-10T08:00:00Z' },
  ], '2026-03-10');
  assert.deepEqual(queue.map((c) => c.loanId), [2, 1]);
  const assigned = svc.assign(queue, [{ agentId: 'a-1', capacity: 5, openCases: 0, seniority: 'JUNIOR' }]);
  assert.equal(assigned[0].assignedAgentId, undefined);
  assert.equal(assigned[1].assignedAgentId, 'a-1');
});

test('payoff quote includes accrued interest through good-through date', () => {
  const svc = new PayoffQuoteService(calendar);
  const loan = aLoan();
  const schedule = new AmortizationService(calendar).buildSchedule(loan);
  const quote = svc.quotePayoff(loan, schedule, [aFee()], '2026-01-20');
  assert.equal(quote.principal, loan.principal);
  assert.equal(quote.perDiem, 395);
  assert.equal(quote.total, quote.principal + quote.accruedInterest + 2_500);
});

test('retry stops on non-transient errors', async () => {
  let calls = 0;
  await assert.rejects(
    withRetry(async () => {
      calls++;
      throw new HttpStatusError(400, 'bad request');
    }, { attempts: 3, baseDelayMs: 1, maxDelayMs: 1, isRetryable: isTransientHttpError, sleep: async () => undefined }),
  );
  assert.equal(calls, 1);
});

test('placeholder credentials are rejected outside the test profile', () => {
  const url = `postgresql://${TEST_DATABASE.user}:${TEST_DATABASE.password}@localhost:55432/servicing_test`;
  process.env.SERVICING_PROFILE = 'test';
  assert.equal(buildDatabaseSettings(url).password, 'changeme');
  process.env.SERVICING_PROFILE = 'staging';
  assert.throws(() => buildDatabaseSettings(url), PlaceholderSecretError);
  delete process.env.SERVICING_PROFILE;
});

test('webhook key default only applies in the test profile', () => {
  const key = loadWebhookSigningKey({ SERVICING_PROFILE: 'test' });
  const signer = new WebhookSigner(key);
  const header = signer.sign('{"loanId":1}', 1_700_000_000);
  assert.equal(signer.verify('{"loanId":1}', header, 1_700_000_100), true);
  assert.throws(() => loadWebhookSigningKey({ SERVICING_PROFILE: 'prod' }), MissingSecretError);
  assert.throws(() => loadWebhookSigningKey({}), MissingSecretError);
});
