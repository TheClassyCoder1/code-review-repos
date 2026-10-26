import { NotFoundError } from '../util/errors';
import { InstallmentRegistry } from './installment-registry';

export interface NextDueView {
  loanId: string;
  dueDate: string | null;
  amountMinor: number;
  paidOff: boolean;
}

/** Backs the "next payment" card in the borrower portal. */
export class NextDueService {
  constructor(private readonly registry: InstallmentRegistry) {}

  nextDue(loanId: string): NextDueView {
    if (!this.registry.has(loanId)) {
      throw new NotFoundError(`no installment plan for loan ${loanId}`);
    }
    const next = this.registry.nextUnpaid(loanId);
    if (!next) {
      return { loanId, dueDate: null, amountMinor: 0, paidOff: true };
    }
    return { loanId, dueDate: next.dueDate, amountMinor: next.amountMinor, paidOff: false };
  }
}
