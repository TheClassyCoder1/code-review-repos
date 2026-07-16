import { Args, Int, Query, Resolver } from '@nestjs/graphql';
import { AggregationService } from '../services/aggregation.service';
import { DashboardModel } from '../models/dashboard.model';

/**
 * GRAPHQL: query dashboard(userId) resolves via the SAME aggregating fan-out
 * (AggregationService.buildDashboard) used by the REST aggregating proxy.
 * Cross-repo edges: portal-bff -> loan-service + risk-service.
 */
@Resolver(() => DashboardModel)
export class DashboardResolver {
  constructor(private readonly aggregation: AggregationService) {}

  @Query(() => DashboardModel)
  async dashboard(@Args('userId', { type: () => Int }) userId: number): Promise<DashboardModel> {
    const d = await this.aggregation.buildDashboard(userId);
    return {
      userId: d.userId,
      loan: d.loan as DashboardModel['loan'],
      risk: d.risk as DashboardModel['risk'],
    };
  }
}
