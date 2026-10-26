import { randomUUID } from 'crypto';
import { Cents, IsoDate, LedgerEntry, LedgerEntryType } from '../domain/types';
import { assertCents } from '../util/money';

export interface NewLedgerEntry {
  loanId: number;
  type: LedgerEntryType;
  amount: Cents;
  effectiveDate: IsoDate;
  reversesEntryId?: string;
  idempotencyKey: string;
}

export class DuplicateReversalError extends Error {
  constructor(readonly entryId: string) {
    super(`ledger entry ${entryId} has already been reversed`);
  }
}

/**
 * Append-only servicing ledger. Entries are never updated or deleted; corrections are
 * posted as reversals. Kept in memory per process, backed by the outbox for durability.
 */
export class LedgerRepository {
  private readonly entriesByLoan = new Map<number, LedgerEntry[]>();
  private readonly byIdempotencyKey = new Map<string, LedgerEntry>();
  private readonly reversed = new Set<string>();

  constructor(private readonly clock: () => Date = () => new Date()) {}

  /**
   * Idempotent append: replaying the same key returns the original entry instead of
   * double-posting. A reversal must point at an existing entry of the same loan with
   * the opposite sign, and each entry can be reversed at most once.
   */
  postEntry(input: NewLedgerEntry): LedgerEntry {
    const existing = this.byIdempotencyKey.get(input.idempotencyKey);
    if (existing) {
      return existing;
    }
    assertCents(input.amount, 'amount');

    if (input.type === LedgerEntryType.Reversal) {
      const target = this.findEntry(input.loanId, input.reversesEntryId);
      if (!target) {
        throw new RangeError(`reversal target ${input.reversesEntryId} not found for loan ${input.loanId}`);
      }
      if (this.reversed.has(target.entryId)) {
        throw new DuplicateReversalError(target.entryId);
      }
      if (input.amount !== -target.amount) {
        throw new RangeError(`reversal amount must be ${-target.amount}, got ${input.amount}`);
      }
      this.reversed.add(target.entryId);
    }

    const entry: LedgerEntry = {
      entryId: randomUUID(),
      loanId: input.loanId,
      type: input.type,
      amount: input.amount,
      effectiveDate: input.effectiveDate,
      postedAt: this.clock().toISOString(),
      reversesEntryId: input.reversesEntryId,
    };
    const list = this.entriesByLoan.get(input.loanId) ?? [];
    list.push(entry);
    this.entriesByLoan.set(input.loanId, list);
    this.byIdempotencyKey.set(input.idempotencyKey, entry);
    return entry;
  }

  /**
   * Balance owed as of the end of `asOf`, by effective date. Back-dated postings are
   * included because effective date, not posting time, drives the balance.
   */
  balanceAsOf(loanId: number, asOf: IsoDate): Cents {
    const entries = this.entriesByLoan.get(loanId) ?? [];
    let balance = 0;
    for (const entry of entries) {
      if (entry.effectiveDate <= asOf) {
        balance += entry.amount;
      }
    }
    return balance;
  }

  entriesBetween(loanId: number, from: IsoDate, to: IsoDate): LedgerEntry[] {
    return (this.entriesByLoan.get(loanId) ?? [])
      .filter((e) => e.effectiveDate >= from && e.effectiveDate <= to)
      .sort((a, b) =>
        a.effectiveDate === b.effectiveDate
          ? a.postedAt.localeCompare(b.postedAt)
          : a.effectiveDate.localeCompare(b.effectiveDate),
      );
  }

  private findEntry(loanId: number, entryId: string | undefined): LedgerEntry | undefined {
    if (!entryId) {
      return undefined;
    }
    return (this.entriesByLoan.get(loanId) ?? []).find((e) => e.entryId === entryId);
  }
}
