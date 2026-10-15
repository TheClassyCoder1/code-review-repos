import { Module } from '@nestjs/common';
import { StatementsController } from './statements.controller';
import { AdminStatementsController } from './admin-statements.controller';
import { StatementsService } from './statements.service';
import { BillingAccountService } from './billing-account.service';
import { ReportExportService } from './report-export.service';
import { UsageImportService } from './usage-import.service';
import { UsageMeterService } from './usage-meter.service';

@Module({
  controllers: [StatementsController, AdminStatementsController],
  providers: [
    StatementsService,
    BillingAccountService,
    ReportExportService,
    UsageImportService,
    UsageMeterService,
  ],
})
export class BillingModule {}
