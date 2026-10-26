'use strict';

const rules = require('./disputeRules');

/**
 * CSV export of the servicing queue for the monthly dispute review. The file
 * is shared with the collections partner, so the payment reference a borrower
 * quoted is masked before it leaves the service.
 */

const COLUMNS = [
  'dispute_id',
  'statement_id',
  'period',
  'reason_code',
  'amount',
  'status',
  'opened_at',
  'respond_by',
  'overdue',
  'reference',
];

const VISIBLE_REFERENCE_CHARS = 4;
const MASK_CHAR = '*';

/**
 * Mask a payment reference for the partner export.
 */
function maskReference(reference) {
  if (!reference) {
    return '';
  }
  const ref = String(reference).replace(/\s+/g, '');
  if (ref.length <= VISIBLE_REFERENCE_CHARS) {
    return MASK_CHAR.repeat(ref.length);
  }
  return ref.slice(0, VISIBLE_REFERENCE_CHARS) + MASK_CHAR.repeat(ref.length - VISIBLE_REFERENCE_CHARS);
}

/**
 * Quote a CSV cell and neutralise spreadsheet formula prefixes.
 */
function csvCell(value) {
  if (value === null || value === undefined) {
    return '';
  }
  let text = String(value);
  if (/^[=+\-@\t\r]/.test(text)) {
    text = `'${text}`;
  }
  if (/[",\n\r]/.test(text)) {
    text = `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

function toRow(dispute, now) {
  return [
    dispute.id,
    dispute.statementId,
    dispute.period,
    dispute.reasonCode,
    Number.isFinite(dispute.amount) ? dispute.amount.toFixed(2) : '',
    dispute.status,
    new Date(dispute.openedAt).toISOString(),
    new Date(dispute.respondBy).toISOString(),
    rules.isOverdue(dispute, now) ? 'Y' : 'N',
    maskReference(dispute.reference),
  ];
}

/**
 * Render the given disputes as CSV with a header row.
 */
function toCsv(disputes, now = Date.now()) {
  const lines = [COLUMNS.join(',')];
  for (const dispute of disputes) {
    lines.push(toRow(dispute, now).map(csvCell).join(','));
  }
  return `${lines.join('\n')}\n`;
}

/**
 * Totals per reason code for the review summary page.
 */
function summarise(disputes) {
  const byReason = {};
  for (const code of Object.keys(rules.REASON_CODES)) {
    byReason[code] = { count: 0, amount: 0 };
  }
  for (const dispute of disputes) {
    const bucket = byReason[dispute.reasonCode];
    if (!bucket) {
      continue;
    }
    bucket.count += 1;
    if (Number.isFinite(dispute.amount)) {
      bucket.amount = Math.round((bucket.amount + dispute.amount) * 100) / 100;
    }
  }
  return byReason;
}

module.exports = { COLUMNS, maskReference, csvCell, toCsv, summarise };
