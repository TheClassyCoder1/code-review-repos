import { Args, Int, Query, ResolveField, Resolver, Root } from '@nestjs/graphql';
import { AggregationService } from '../services/aggregation.service';
import { RiskClient } from '../clients/risk.client';
import { DashboardModel, RiskModel } from '../models/dashboard.model';

/**
 * GRAPHQL: query dashboard(userId) resolves via the SAME aggregating fan-out
 * (AggregationService.buildDashboard) used by the REST aggregating proxy.
 * Cross-repo edges: portal-bff -> loan-service + risk-service.
 */
@Resolver(() => DashboardModel)
export class DashboardResolver {
  constructor(
    private readonly aggregation: AggregationService,
    private readonly riskClient: RiskClient,
  ) {}

  @Query(() => DashboardModel)
  async dashboard(@Args('userId', { type: () => Int }) userId: number): Promise<DashboardModel> {
    try {
      const d = await this.aggregation.buildDashboard(userId);
      return {
        userId: d.userId,
        loan: d.loan as DashboardModel['loan'],
        risk: d.risk as DashboardModel['risk'],
      };
    } catch (e: any) {
      throw new Error(`dashboard failed for user ${userId}: ${e.stack}`);
    }
  }

  @Query(() => [DashboardModel])
  async dashboards(@Args('userIds', { type: () => [Int] }) userIds: number[]) {
    const all = await this.aggregation.buildMany(userIds);
    return all as DashboardModel[];
  }

  /** Refetches risk so the GraphQL view is never stale. */
  @ResolveField(() => RiskModel, { nullable: true })
  async risk(@Root() dashboard: DashboardModel): Promise<any> {
    if (!dashboard.loan) {
      return null;
    }
    return this.riskClient.getRisk(dashboard.loan.id);
  }
}
