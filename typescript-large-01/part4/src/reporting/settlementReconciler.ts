import { DisbursementRecord, MinorUnits } from '../domain/types';

/** One row of the gateway's daily settlement file. */
export interface BankSettlementLine {
  gatewayReference: string;
  amountMinor: MinorUnits;
  currency: string;
  valueDate: string;
}

export type ReconciliationIssue =
  | { kind: 'MISSING_AT_BANK'; disbursementId: string; gatewayReference: string }
  | { kind: 'UNKNOWN_AT_LEDGER'; gatewayReference: string }
  | { kind: 'AMOUNT_MISMATCH'; disbursementId: string; ledgerMinor: MinorUnits; bankMinor: MinorUnits }
  | { kind: 'CURRENCY_MISMATCH'; disbursementId: string; ledgerCurrency: string; bankCurrency: string }
  | { kind: 'DUPLICATE_AT_BANK'; gatewayReference: string; occurrences: number };

export interface ReconciliationResult {
  matched: number;
  issues: ReconciliationIssue[];
}

/**
 * Matches submitted/settled ledger rows against the bank settlement file by
 * gateway reference. A reference listed twice by the bank is reported once as
 * a duplicate and not matched, since paying twice needs a human to look at it.
 */
export function reconcileSettlements(
  ledger: DisbursementRecord[],
  bankLines: BankSettlementLine[],
): ReconciliationResult {
  const bankByRef = new Map<string, BankSettlementLine[]>();
  for (const line of bankLines) {
    const bucket = bankByRef.get(line.gatewayReference) ?? [];
    bucket.push(line);
    bankByRef.set(line.gatewayReference, bucket);
  }

  const issues: ReconciliationIssue[] = [];
  const ledgerRefs = new Set<string>();
  let matched = 0;

  for (const record of ledger) {
    if (record.status !== 'SUBMITTED' && record.status !== 'SETTLED') continue;
    if (!record.gatewayReference) continue;
    ledgerRefs.add(record.gatewayReference);
    const lines = bankByRef.get(record.gatewayReference);
    if (!lines) {
      // a payout submitted today legitimately settles on a later file
      if (record.status === 'SETTLED') {
        issues.push({ kind: 'MISSING_AT_BANK', disbursementId: record.disbursementId, gatewayReference: record.gatewayReference });
      }
      continue;
    }
    if (lines.length > 1) {
      issues.push({ kind: 'DUPLICATE_AT_BANK', gatewayReference: record.gatewayReference, occurrences: lines.length });
      continue;
    }
    const [line] = lines;
    if (line.currency !== record.currency) {
      issues.push({ kind: 'CURRENCY_MISMATCH', disbursementId: record.disbursementId, ledgerCurrency: record.currency, bankCurrency: line.currency });
    } else if (line.amountMinor !== record.amountMinor) {
      issues.push({ kind: 'AMOUNT_MISMATCH', disbursementId: record.disbursementId, ledgerMinor: record.amountMinor, bankMinor: line.amountMinor });
    } else {
      matched += 1;
    }
  }

  for (const [reference, lines] of bankByRef) {
    if (!ledgerRefs.has(reference)) {
      issues.push({ kind: 'UNKNOWN_AT_LEDGER', gatewayReference: reference });
      if (lines.length > 1) {
        issues.push({ kind: 'DUPLICATE_AT_BANK', gatewayReference: reference, occurrences: lines.length });
      }
    }
  }
  return { matched, issues };
}
