import { Module } from '@nestjs/common';
import { RepaymentController } from './controllers/repayment.controller';
import { RepaymentClient } from './clients/repayment.client';
import { RiskTrendClient } from './clients/risk-trend.client';
import { RepaymentOverviewService } from './services/repayment-overview.service';

/** Repayment schedule pages. Import into AppModule to expose the /portal/loans/:id/* routes. */
@Module({
  controllers: [RepaymentController],
  providers: [RepaymentClient, RiskTrendClient, RepaymentOverviewService],
  exports: [RepaymentOverviewService],
})
export class RepaymentModule {}
