import { Controller, Get, Param } from '@nestjs/common';
import { AggregationService } from '../services/aggregation.service';
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
    private readonly loanClient: LoanClient,
  ) {}

  @Get('loans/:id')
  async loan(@Param('id') id: string): Promise<PortalLoanView> {
    const raw = await this.loanClient.getLoan(Number(id));
    return this.aggregation.transform(raw);
  }

  @Get('dashboard/:userId')
  async dashboard(@Param('userId') userId: string): Promise<Dashboard> {
    return this.aggregation.buildDashboard(Number(userId));
  }
}
