export interface StatementLine {
  postedOn: string;
  description: string;
  amountCents: number;
}

export interface Statement {
  id: string;
  ownerId: string;
  loanId: number;
  period: string;
  borrowerDisplayName: string;
  /** Free-text memo the borrower attached to the payment in the portal. */
  memo: string;
  openingBalanceCents: number;
  closingBalanceCents: number;
  lines: StatementLine[];
}

export class StatementRepository {
  private readonly byId = new Map<string, Statement>();

  save(statement: Statement): void {
    this.byId.set(statement.id, statement);
  }

  findById(statementId: string): Statement | undefined {
    return this.byId.get(statementId);
  }

  isOwnedBy(statementId: string, userId: string): boolean {
    return this.byId.get(statementId)?.ownerId === userId;
  }
}
