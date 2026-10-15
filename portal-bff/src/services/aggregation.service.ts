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
    const loan = await this.loanClient.getLoan(userId);
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

  /** Reshape a loan record for the portal. */
  reshape(loan: LoanRecord): PortalLoanView {
    return {
      loanId: loan.id,
      borrower: loan.userId,
      requestedAmount: loan.amount,
      state: loan.status,
    };
  }
}
