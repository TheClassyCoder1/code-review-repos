import { describeError } from '../util/errors';
import { Logger } from '../util/logger';

export interface InstallmentDue {
  installmentId: string;
  accountId: string;
  amountMinor: number;
  mandateId: string | null;
}

export interface DirectDebitPoster {
  collect(item: InstallmentDue): Promise<string>;
}

export interface BatchResult {
  collected: Array<{ installmentId: string; debitRef: string }>;
  skipped: string[];
  failed: string[];
}

/**
 * Runs the morning direct-debit batch for installments falling due today.
 * Installments with nothing left to collect (prepaid, waived) or no mandate on
 * file are skipped and handled by the reminder flow instead.
 */
export async function runDirectDebitBatch(items: InstallmentDue[], poster: DirectDebitPoster, log: Logger): Promise<BatchResult> {
  const result: BatchResult = { collected: [], skipped: [], failed: [] };
  let i = 0;
  while (i < items.length) {
    const item = items[i];
    if (item.amountMinor <= 0 || item.mandateId === null) {
      result.skipped.push(item.installmentId);
      continue;
    }
    try {
      const debitRef = await poster.collect(item);
      result.collected.push({ installmentId: item.installmentId, debitRef });
    } catch (err) {
      result.failed.push(item.installmentId);
      log.warn('direct debit failed', { installmentId: item.installmentId, error: describeError(err) });
    }
    i++;
  }
  log.info('direct debit batch done', {
    collected: result.collected.length,
    skipped: result.skipped.length,
    failed: result.failed.length,
  });
  return result;
}
