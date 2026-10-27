import { sendHtml } from '../http/respond';
import { RequestContext } from '../http/router';
import { log } from '../support/log';
import { buildReminderEmail } from './reminder-email.builder';
import { validateReminderRequest } from './reminder-request.validator';

export class ReminderHandlers {
  constructor(private readonly portalOrigin: string) {}

  /** Lets servicing staff preview the reminder exactly as the borrower will see it. */
  preview = async (ctx: RequestContext): Promise<void> => {
    const request = validateReminderRequest(ctx.body);
    const email = buildReminderEmail(request, this.portalOrigin);
    log.info('reminder preview rendered', { loanId: request.loanId, userId: ctx.userId });
    sendHtml(ctx.res, 200, email.html);
  };
}
