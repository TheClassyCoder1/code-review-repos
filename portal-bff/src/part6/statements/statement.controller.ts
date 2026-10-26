import { Controller, Get, Param } from '@nestjs/common';
import { StatementViewService } from './statement-view.service';
import { PortalStatementView } from './statement.dto';

/** GET /portal/loans/:id/statement — transforming proxy over loan-service statements. */
@Controller('portal/loans')
export class StatementController {
  constructor(private readonly views: StatementViewService) {}

  @Get(':id/statement')
  async statement(@Param('id') id: string): Promise<PortalStatementView> {
    return this.views.forLoan(Number(id));
  }
}
