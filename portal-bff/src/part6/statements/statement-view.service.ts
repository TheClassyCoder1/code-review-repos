import { Injectable, NotFoundException } from '@nestjs/common';
import { daysBetween, formatIsoDay } from '../common/date-format';
import { StatementClient } from './statement.client';
import { LoanStatement, PortalStatementView } from './statement.dto';

const currency = new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'GBP' });

/** Reshapes a loan-service statement into the compact card shown on the portal home page. */
@Injectable()
export class StatementViewService {
  constructor(private readonly statements: StatementClient) {}

  async forLoan(loanId: number, now: Date = new Date()): Promise<PortalStatementView> {
    const statement = await this.statements.latest(loanId);
    if (!statement) {
      throw new NotFoundException(`no statement issued yet for loan ${loanId}`);
    }
    return this.toView(statement, now);
  }

  toView(statement: LoanStatement, now: Date): PortalStatementView {
    const due = new Date(statement.dueDate);
    const daysUntilDue = daysBetween(now, due);
    return {
      loanId: statement.loanId,
      period: `${formatIsoDay(new Date(statement.periodStart))} – ${formatIsoDay(new Date(statement.periodEnd))}`,
      balance: currency.format(statement.closingBalance),
      minimumDue: currency.format(statement.minimumPayment),
      dueOn: formatIsoDay(due),
      daysUntilDue,
      overdue: daysUntilDue < 0 && statement.minimumPayment > 0,
      lineCount: statement.lines.length,
    };
  }
}
