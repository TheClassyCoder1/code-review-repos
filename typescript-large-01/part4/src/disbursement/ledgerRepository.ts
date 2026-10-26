import { DisbursementRecord, DisbursementStatus } from '../domain/types';

/**
 * Storage port for disbursement records. The production adapter is backed by
 * the servicing database; the in-memory adapter below is used by the batch
 * simulator and local runs.
 */
export interface DisbursementRepository {
  findOpenByLoan(loanId: number): Promise<DisbursementRecord | null>;
  findById(disbursementId: string): Promise<DisbursementRecord | null>;
  insert(record: DisbursementRecord): Promise<void>;
  update(record: DisbursementRecord): Promise<void>;
  listByStatus(status: DisbursementStatus): Promise<DisbursementRecord[]>;
}

const OPEN_STATUSES: ReadonlySet<DisbursementStatus> = new Set(['REQUESTED', 'RESERVED', 'SUBMITTED']);

export class InMemoryDisbursementRepository implements DisbursementRepository {
  private readonly rows = new Map<string, DisbursementRecord>();

  async findOpenByLoan(loanId: number): Promise<DisbursementRecord | null> {
    for (const row of this.rows.values()) {
      if (row.loanId === loanId && OPEN_STATUSES.has(row.status)) {
        return { ...row };
      }
    }
    return null;
  }

  async findById(disbursementId: string): Promise<DisbursementRecord | null> {
    const row = this.rows.get(disbursementId);
    return row ? { ...row } : null;
  }

  async insert(record: DisbursementRecord): Promise<void> {
    if (this.rows.has(record.disbursementId)) {
      throw new Error(`disbursement ${record.disbursementId} already exists`);
    }
    this.rows.set(record.disbursementId, { ...record });
  }

  async update(record: DisbursementRecord): Promise<void> {
    if (!this.rows.has(record.disbursementId)) {
      throw new Error(`disbursement ${record.disbursementId} does not exist`);
    }
    this.rows.set(record.disbursementId, { ...record });
  }

  async listByStatus(status: DisbursementStatus): Promise<DisbursementRecord[]> {
    return [...this.rows.values()].filter((row) => row.status === status).map((row) => ({ ...row }));
  }
}
