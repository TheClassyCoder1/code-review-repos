'use strict';

const crypto = require('crypto');
const { collectionsConfig } = require('./config');
const { ageLoan } = require('./delinquency');
const { withSpoolFile } = require('./spoolScope');
const { writeRows } = require('./spoolFile');
const { startProgressTicker, stopProgressTicker } = require('./progressTicker');
const { logger } = require('../utils/logger');

/**
 * Nightly delinquency export. Ages every loan with an unpaid installment,
 * writes the delinquent ones to a spool CSV for the collections uploader, and
 * returns a summary for the job log.
 */

/**
 * Group a flat list of installment rows by loan id, preserving the order in
 * which loans first appear. Rows without a loan id are counted and skipped
 * rather than failing the whole run.
 */
function groupInstallmentsByLoan(rows) {
  const byLoan = new Map();
  let skipped = 0;

  for (const row of rows) {
    if (!row || typeof row.loanId !== 'string' || row.loanId.length === 0) {
      skipped += 1;
      continue;
    }
    const list = byLoan.get(row.loanId);
    if (list) {
      list.push(row);
    } else {
      byLoan.set(row.loanId, [row]);
    }
  }

  if (skipped > 0) {
    logger.warn({ skipped }, 'delinquency.rows.skipped');
  }
  return byLoan;
}

/**
 * Count aged loans per bucket, including empty buckets so dashboards always
 * receive the same keys.
 */
function bucketCounts(aged, bucketNames) {
  const counts = Object.fromEntries(bucketNames.map((name) => [name, 0]));
  for (const record of aged) {
    counts[record.bucket] = (counts[record.bucket] || 0) + 1;
  }
  return counts;
}

/**
 * Run the export for `asOfDay` over the given installment rows.
 */
async function runDelinquencyExport(rows, asOfDay, options = {}) {
  const spoolDir = options.spoolDir || collectionsConfig.spoolDir;
  const exportId = options.exportId || `delinq-${asOfDay}-${crypto.randomBytes(4).toString('hex')}`;
  const byLoan = groupInstallmentsByLoan(rows);
  const progress = { processed: 0, total: byLoan.size };

  const ticker = startProgressTicker(
    'delinquency-export',
    progress,
    options.heartbeatMs || collectionsConfig.delinquency.heartbeatMs
  );
  try {
    const aged = [];
    for (const [loanId, installments] of byLoan) {
      aged.push(ageLoan(loanId, installments, asOfDay));
      progress.processed += 1;
    }
    const delinquent = aged.filter((record) => record.daysPastDue > 0);

    const { result: written, filePath } = await withSpoolFile(spoolDir, exportId, (handle) =>
      writeRows(handle, delinquent)
    );

    const summary = {
      exportId,
      asOfDay,
      loans: aged.length,
      delinquent: written,
      buckets: bucketCounts(aged, ['current', '1-29', '30-59', '60-89', '90+']),
      filePath,
    };
    logger.info({ exportId, loans: summary.loans, delinquent: written }, 'delinquency.export.done');
    return summary;
  } finally {
    stopProgressTicker(ticker);
  }
}

module.exports = { runDelinquencyExport, groupInstallmentsByLoan, bucketCounts };
