import { HttpError, requirePositiveInt, sendHtml, sendJson } from '../http/respond';
import { RequestContext } from '../http/router';
import { parseSortSpec } from './sort-spec';
import { toSafeStatementModel } from './statement-model';
import { StatementRepository } from './statement.repository';
import { StatementSearchRepository } from './statement-search.repository';
import { renderStatementPage } from './statement.template';

export class StatementHandlers {
  constructor(
    private readonly statements: StatementRepository,
    private readonly search: StatementSearchRepository,
  ) {}

  list = async (ctx: RequestContext): Promise<void> => {
    const loanIdRaw = ctx.query.get('loanId');
    const rows = await this.search.search({
      ownerId: ctx.userId as string,
      loanId: loanIdRaw ? requirePositiveInt(loanIdRaw, 'loanId') : undefined,
      sort: parseSortSpec(ctx.query.get('sort')),
      limit: Number(ctx.query.get('limit') ?? 24),
    });
    sendJson(ctx.res, 200, { statements: rows });
  };

  get = async (ctx: RequestContext): Promise<void> => {
    const statement = this.statements.findById(ctx.params.statementId);
    if (!statement) {
      throw new HttpError(404, 'statement not found');
    }
    const { borrowerDisplayName, memo, ownerId, ...summary } = statement;
    sendJson(ctx.res, 200, summary);
  };

  view = async (ctx: RequestContext): Promise<void> => {
    const found = this.statements.findById(ctx.params.statementId);
    if (!found) {
      throw new HttpError(404, 'statement not found');
    }
    sendHtml(ctx.res, 200, renderStatementPage(toSafeStatementModel(found)));
  };
}
