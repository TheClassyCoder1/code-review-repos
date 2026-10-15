import { Injectable, NotFoundException } from '@nestjs/common';
import { Statement, StatementPatch } from './billing.dto';
import { STATEMENT_PAGE_SIZE } from './billing.constants';

@Injectable()
export class StatementsService {
  private readonly statements = new Map<string, Statement>();

  findStatement(accountId: string, id: string): Statement {
    const statement = this.statements.get(id);
    if (!statement || statement.accountId !== accountId) {
      throw new NotFoundException(`statement ${id} not found`);
    }
    return statement;
  }

  listStatements(accountId: string, page: number): { items: Statement[]; total: number } {
    const owned = [...this.statements.values()]
      .filter((s) => s.accountId === accountId)
      .sort((a, b) => b.period.localeCompare(a.period));
    const safePage = Math.max(1, Math.floor(page) || 1);
    const start = (safePage - 1) * STATEMENT_PAGE_SIZE;
    return { items: owned.slice(start, start + STATEMENT_PAGE_SIZE), total: owned.length };
  }

  statementTotalCents(statement: Statement): number {
    let total = 0;
    for (const line of statement.lines) {
      total += line.amountCents * line.quantity;
    }
    return total;
  }

  updateStatement(accountId: string, id: string, patch: StatementPatch): Statement {
    const statement = this.findStatement(accountId, id);
    if (statement.status !== 'draft') {
      throw new NotFoundException(`statement ${id} is not editable`);
    }
    Object.assign(statement, patch);
    this.statements.set(id, statement);
    return statement;
  }

  voidStatement(id: string, reason: string): Statement {
    const statement = this.statements.get(id);
    if (!statement) {
      throw new NotFoundException(`statement ${id} not found`);
    }
    statement.status = 'void';
    statement.memo = `Voided: ${reason}`;
    return statement;
  }
}
