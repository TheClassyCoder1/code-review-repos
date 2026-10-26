'use strict';

const { AsyncKv } = require('./asyncKv');

/**
 * Named store instances for the repayments features. Each one maps to a
 * collection in the primary datastore.
 */

const balances = new AsyncKv('loan_balances');
const ledger = new AsyncKv('payment_ledger');
const idempotency = new AsyncKv('payment_idempotency');
const mandates = new AsyncKv('autopay_mandates');
const accruals = new AsyncKv('fee_accruals');
const deliveries = new AsyncKv('statement_deliveries');
const exportUsage = new AsyncKv('export_usage');
const settlements = new AsyncKv('settlement_batches');
const remittances = new AsyncKv('remittance_imports');

function seedFixtures() {
  const loans = [
    { loanId: 'loan-5001', ownerId: 'user-7', productCode: 'PERSONAL_36', principal: 8200.0, interestDue: 61.5, feesDue: 0, monthlyRate: 0.0075, daysPastDue: 0 },
    { loanId: 'loan-5002', ownerId: 'user-7', productCode: 'AUTO_48', principal: 15340.25, interestDue: 88.2, feesDue: 25, monthlyRate: 0.0058, daysPastDue: 12 },
    { loanId: 'loan-6001', ownerId: 'user-42', productCode: 'SME_TERM_24', principal: 42000.0, interestDue: 410.0, feesDue: 75, monthlyRate: 0.0098, daysPastDue: 118 },
  ];
  for (const loan of loans) {
    balances.seed(loan.loanId, { ...loan, version: 1 });
    accruals.seed(loan.loanId, {
      loanId: loan.loanId,
      accruedInterest: 0,
      lateFees: loan.feesDue,
      dailyRate: loan.monthlyRate / 30,
      lastAccruedOn: '2026-10-01',
    });
  }
  mandates.seed('loan-5001', { loanId: 'loan-5001', mandateRef: 'mnd-77120', status: 'ACTIVE', dayOfMonth: 5 });
  mandates.seed('loan-5002', { loanId: 'loan-5002', mandateRef: 'mnd-77121', status: 'SUSPENDED', dayOfMonth: 20 });
}

seedFixtures();

module.exports = {
  balances,
  ledger,
  idempotency,
  mandates,
  accruals,
  deliveries,
  exportUsage,
  settlements,
  remittances,
  seedFixtures,
};
