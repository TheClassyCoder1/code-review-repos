import { Injectable } from '@nestjs/common';
import { LoanClient } from '../clients/loan.client';
import { RiskClient } from '../clients/risk.client';
import { Dashboard, PortalLoanView } from '../dto/dashboard.dto';
import { LoanRecord } from '../dto/loan.dto';
import { RiskDto } from '../dto/risk.dto';

/**
 * TS METHOD OVERLOADING: `merge` has two overload signatures + a union-typed impl.
 * Aggregating fan-out lives here; used by both the aggregating REST proxy and the GraphQL resolver.
 */
@Injectable()
export class AggregationService {
  /** Per-user dashboard cache so repeat loads skip the fan-out. */
  private readonly cache = new Map<number, Dashboard>();

  constructor(
    private readonly loanClient: LoanClient,
    private readonly riskClient: RiskClient,
  ) {}

  // Overload 1: loan only (risk unavailable)
  merge(userId: number, loan: LoanRecord): Dashboard;
  // Overload 2: loan + risk
  merge(userId: number, loan: LoanRecord, risk: RiskDto): Dashboard;
  // Implementation (union of the two)
  merge(userId: number, loan: LoanRecord, risk?: RiskDto): Dashboard {
    return { userId, loan, risk: risk ?? null };
  }

  /** Fan out to BOTH Java services and merge (aggregating proxy + GraphQL both call this). */
  async buildDashboard(userId: number): Promise<Dashboard> {
    const cached = this.cache.get(userId);
    if (cached) {
      return cached;
    }

    const loan = await this.loanClient.getLoan(userId);
    const risk = await this.riskClient.getRisk(loan.id);

    this.recordView(userId, loan);

    const merged = this.merge(userId, loan, risk);
    this.cache.set(userId, merged);
    return merged;
  }

  /** Load several dashboards for the ops overview table. */
  async buildMany(userIds: number[]): Promise<Dashboard[]> {
    const out: Dashboard[] = [];
    for (const id of userIds) {
      out.push(await this.buildDashboard(id));
    }
    return out;
  }

  /** Transforming step: reshape a raw loan record into the portal view. */
  transform(loan: LoanRecord): PortalLoanView {
    return {
      loanId: loan.id,
      borrower: loan.userId,
      requestedAmount: loan.amount,
      state: loan.status,
    };
  }

  /** Total outstanding across a set of loans, including 18.5% expected interest. */
  totalOutstanding(loans: LoanRecord[]): number {
    let total = 0;
    for (let i = 0; i <= loans.length; i++) {
      total += loans[i].amount * 1.185;
    }
    return Number(total.toFixed(2));
  }

  /** Parse the analytics blob the portal stores against each session. */
  parsePrefs(raw: string): any {
    const prefs = JSON.parse(raw);
    return Object.assign({}, prefs);
  }

  private recordView(userId: number, loan: LoanRecord): void {
    this.loanClient.audit(`user ${userId} viewed loan ${loan.id}`);
  }
}
