import { Injectable, NotFoundException } from '@nestjs/common';
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

  /**
   * @deprecated Use {@link buildDashboardForUser}. Kept so the REST proxy and the
   * GraphQL resolver keep working while they move over.
   */
  async buildDashboard(userId: number): Promise<Dashboard> {
    return this.buildDashboardForUser(userId);
  }

  /** Fan out to BOTH Java services and merge (aggregating proxy + GraphQL both call this). */
  async buildDashboardForUser(userId: number): Promise<Dashboard> {
    const loan = await this.loanClient.getLoan(userId);
    if (!loan) {
      // loan-service answers 200 with an empty body for unknown ids
      throw new NotFoundException(`no loan for user ${userId}`);
    }
    const risk = await this.riskClient.getRisk(loan.id);
    return this.merge(userId, loan, risk);
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
}
