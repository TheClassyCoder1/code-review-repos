import { Body, Controller, Param, Post, UseGuards } from '@nestjs/common';
import { AdminGuard } from '../common/admin.guard';
import { Statement } from './billing.dto';
import { StatementsService } from './statements.service';

@Controller('billing/admin')
@UseGuards(AdminGuard)
export class AdminStatementsController {
  constructor(private readonly statements: StatementsService) {}

  @Post('statements/:id/void')
  voidStatement(@Param('id') id: string, @Body('reason') reason: string): Statement {
    const cleanReason = (reason ?? '').trim().slice(0, 200) || 'no reason given';
    return this.statements.voidStatement(id, cleanReason);
  }
}
