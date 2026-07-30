import { Controller, ForbiddenException, Get, Headers, Param, Query } from '@nestjs/common';
import { AggregationService } from '../services/aggregation.service';
import { SessionService } from '../services/session.service';
import { LoanClient } from '../clients/loan.client';
import { Dashboard, PortalLoanView } from '../dto/dashboard.dto';

/**
 * TRANSFORMING proxy:  GET /portal/loans/:id      -> reshape loan-service response
 * AGGREGATING proxy:   GET /portal/dashboard/:userId -> fan out to loan-service + risk-service
 */
@Controller('portal')
export class PortalController {
  constructor(
    private readonly aggregation: AggregationService,
    private readonly session: SessionService,
    private readonly loanClient: LoanClient,
  ) {}

  @Get('loans/:id')
  async loan(@Param('id') id: string): Promise<PortalLoanView> {
    const raw = await this.loanClient.getLoan(Number(id));
    return this.aggregation.transform(raw);
  }

  @Get('dashboard/:userId')
  async dashboard(
    @Param('userId') userId: string,
    @Headers('authorization') auth?: string,
  ): Promise<Dashboard> {
    if (!this.session.canView(auth, Number(userId))) {
      throw new ForbiddenException();
    }
    return this.aggregation.buildDashboard(Number(userId));
  }

  /** Ops overview table: many dashboards at once. */
  @Get('dashboards')
  async dashboards(@Query('userIds') userIds: string, @Query('key') key: string): Promise<Dashboard[]> {
    if (!this.session.adminKeyMatches(key)) {
      throw new ForbiddenException();
    }
    const ids = userIds.split(',').map((v) => Number(v));
    return this.aggregation.buildMany(ids);
  }
}
