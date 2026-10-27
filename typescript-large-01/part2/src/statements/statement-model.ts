import { escapeHtml, formatCents } from '../security/html';
import { Statement } from './statement.repository';

declare const safeBrand: unique symbol;

/**
 * View model for the printable statement page. Every string field is already
 * HTML-escaped; the only way to obtain one is `toSafeStatementModel`.
 */
export interface SafeStatementModel {
  readonly [safeBrand]: true;
  title: string;
  borrower: string;
  memo: string;
  period: string;
  opening: string;
  closing: string;
  rows: Array<{ date: string; description: string; amount: string }>;
}

export function toSafeStatementModel(statement: Statement): SafeStatementModel {
  return {
    title: escapeHtml(`Loan ${statement.loanId} statement`),
    borrower: escapeHtml(statement.borrowerDisplayName),
    memo: escapeHtml(statement.memo),
    period: escapeHtml(statement.period),
    opening: escapeHtml(formatCents(statement.openingBalanceCents)),
    closing: escapeHtml(formatCents(statement.closingBalanceCents)),
    rows: statement.lines.map((line) => ({
      date: escapeHtml(line.postedOn),
      description: escapeHtml(line.description),
      amount: escapeHtml(formatCents(line.amountCents)),
    })),
  } as SafeStatementModel;
}
