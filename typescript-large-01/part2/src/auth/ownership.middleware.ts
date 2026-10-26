import { HttpError } from '../http/respond';
import { Middleware } from '../http/router';
import { StatementRepository } from '../statements/statement.repository';

/**
 * Gate for every `/portal/statements/:statementId` route. Must run after
 * `authenticate`. Statements belonging to other borrowers are reported as
 * missing so their ids cannot be probed.
 */
export function requireStatementOwner(statements: StatementRepository): Middleware {
  return async (ctx, next) => {
    const statementId = ctx.params.statementId;
    if (!ctx.userId || !statementId || !statements.isOwnedBy(statementId, ctx.userId)) {
      throw new HttpError(404, 'statement not found');
    }
    await next();
  };
}
