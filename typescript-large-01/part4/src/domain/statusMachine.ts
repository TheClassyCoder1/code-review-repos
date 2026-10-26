import { DisbursementRecord, DisbursementStatus } from './types';

const ALLOWED: Record<DisbursementStatus, readonly DisbursementStatus[]> = {
  REQUESTED: ['RESERVED', 'CANCELLED'],
  RESERVED: ['SUBMITTED', 'CANCELLED'],
  SUBMITTED: ['SETTLED', 'FAILED'],
  SETTLED: [],
  FAILED: ['RESERVED'],
  CANCELLED: [],
};

export class IllegalTransitionError extends Error {
  constructor(readonly from: DisbursementStatus, readonly to: DisbursementStatus) {
    super(`cannot move disbursement from ${from} to ${to}`);
    this.name = 'IllegalTransitionError';
  }
}

export function isTerminal(status: DisbursementStatus): boolean {
  return ALLOWED[status].length === 0;
}

/**
 * Returns a copy of `record` in the `next` state. A transition to the current
 * state is treated as an idempotent replay and returns the record unchanged.
 * SUBMITTED and SETTLED require a gateway reference; it is kept once assigned.
 */
export function applyTransition(
  record: DisbursementRecord,
  next: DisbursementStatus,
  nowIso: string,
  gatewayReference?: string,
): DisbursementRecord {
  if (record.status === next) {
    return record;
  }
  if (!ALLOWED[record.status].includes(next)) {
    throw new IllegalTransitionError(record.status, next);
  }

  const reference = gatewayReference ?? record.gatewayReference;
  if ((next === 'SUBMITTED' || next === 'SETTLED') && !reference) {
    throw new Error(`transition to ${next} requires a gateway reference`);
  }
  if (gatewayReference && record.gatewayReference && gatewayReference !== record.gatewayReference) {
    throw new Error('gateway reference cannot change once assigned');
  }

  return {
    ...record,
    status: next,
    gatewayReference: next === 'RESERVED' ? null : reference,
    updatedAt: nowIso,
  };
}
