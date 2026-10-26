export interface DisbursementRecord {
  idempotencyKey: string;
  loanId: string;
  gatewayPayoutId: string;
  amountMinor: number;
  status: 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}

/** Durable lookup of disbursements by client-supplied idempotency key. */
export interface IdempotencyStore {
  find(key: string): Promise<DisbursementRecord | undefined>;
  save(key: string, record: DisbursementRecord): Promise<void>;
}
