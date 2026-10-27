import { HttpError } from '../http/respond';
import { RequestContext } from '../http/router';
import { errorMessage, log } from '../support/log';
import { ReportRunner } from './report-runner';
import { parseReportRequest } from './report-type.guard';

export class ReportHandlers {
  constructor(
    private readonly runner: ReportRunner,
    private readonly canAccessLoan: (userId: string, loanId: number) => boolean,
  ) {}

  download = async (ctx: RequestContext): Promise<void> => {
    const request = parseReportRequest(ctx.params, ctx.query);
    if (!this.canAccessLoan(ctx.userId as string, Number(request.loanId))) {
      throw new HttpError(404, 'loan not found');
    }
    const pdf = await this.runner.render(request).catch((err) => {
      log.error('report rendering failed', { loanId: request.loanId, type: request.type, error: errorMessage(err) });
      throw new HttpError(502, 'report could not be generated');
    });
    ctx.res.writeHead(200, {
      'Content-Type': 'application/pdf',
      'Content-Disposition': `attachment; filename="${request.type}-${request.loanId}-${request.year}.pdf"`,
    });
    ctx.res.end(pdf);
  };
}
