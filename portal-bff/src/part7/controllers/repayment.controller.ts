import { BadRequestException, Controller, Get, Param, Query } from '@nestjs/common';
import { RepaymentClient } from '../clients/repayment.client';
import { RepaymentOverviewService } from '../services/repayment-overview.service';
import { PayoffQuoteRecord, RepaymentOverview, RepaymentScheduleRecord } from '../dto/repayment.dto';

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * PASS-THROUGH:  GET /portal/loans/:id/schedule -> loan-service GET /api/v1/loans/{id}/schedule
 * PASS-THROUGH:  GET /portal/loans/:id/payoff   -> loan-service GET /api/v1/loans/{id}/schedule/payoff
 * AGGREGATING:   GET /portal/loans/:id/repayments -> loan-service schedule + risk-service trend
 */
@Controller('portal/loans')
export class RepaymentController {
  constructor(
    private readonly repaymentClient: RepaymentClient,
    private readonly overviewService: RepaymentOverviewService,
  ) {}

  @Get(':id/schedule')
  async schedule(@Param('id') id: string): Promise<RepaymentScheduleRecord | null> {
    return this.repaymentClient.getSchedule(parseLoanId(id));
  }

  @Get(':id/payoff')
  async payoff(@Param('id') id: string, @Query('on') on?: string): Promise<PayoffQuoteRecord> {
    if (on !== undefined && !ISO_DATE.test(on)) {
      throw new BadRequestException('on must be an ISO date (YYYY-MM-DD)');
    }
    return this.repaymentClient.getPayoffQuote(parseLoanId(id), on);
  }

  @Get(':id/repayments')
  async repayments(@Param('id') id: string): Promise<RepaymentOverview> {
    return this.overviewService.overview(parseLoanId(id));
  }
}

function parseLoanId(raw: string): number {
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) {
    throw new BadRequestException('loan id must be a positive integer');
  }
  return id;
}
