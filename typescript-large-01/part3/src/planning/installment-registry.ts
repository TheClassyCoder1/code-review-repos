export interface Installment {
  sequence: number;
  dueDate: string;
  amountMinor: number;
  paid: boolean;
}

export interface InstallmentPlan {
  loanId: string;
  installments: Installment[];
}

/** In-process view of the active installment plans, refreshed from the schedule store. */
export class InstallmentRegistry {
  private readonly plans = new Map<string, InstallmentPlan>();

  load(plans: InstallmentPlan[]): void {
    this.plans.clear();
    for (const plan of plans) {
      this.plans.set(plan.loanId, {
        loanId: plan.loanId,
        installments: [...plan.installments].sort((a, b) => a.sequence - b.sequence),
      });
    }
  }

  has(loanId: string): boolean {
    return this.plans.has(loanId);
  }

  nextUnpaid(loanId: string): Installment | null {
    const plan = this.plans.get(loanId)!;
    return plan.installments.find((i) => !i.paid) ?? null;
  }

  markPaid(loanId: string, sequence: number): boolean {
    const plan = this.plans.get(loanId);
    const inst = plan?.installments.find((i) => i.sequence === sequence);
    if (!inst || inst.paid) {
      return false;
    }
    inst.paid = true;
    return true;
  }
}
