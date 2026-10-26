import { randomUUID } from 'crypto';
import { findPartner } from '../config/partners';
import { HttpError, sendRedirect } from '../http/respond';
import { RequestContext } from '../http/router';
import { log } from '../support/log';

/**
 * Sends the borrower back to the originating dealer/broker once they have
 * finished the portal step, tagging the hop with an opaque reference the
 * partner can use to pull the application status server-to-server.
 */
export class PartnerHandoffHandler {
  complete = async (ctx: RequestContext): Promise<void> => {
    const partner = findPartner(ctx.params.partnerId);
    if (!partner) {
      throw new HttpError(404, 'unknown partner');
    }
    const product = ctx.query.get('product') ?? partner.products[0];
    if (!partner.products.includes(product)) {
      throw new HttpError(400, 'product not offered through this partner');
    }
    const handoffRef = randomUUID();
    log.info('partner handoff', { partnerId: partner.id, userId: ctx.userId, ref: handoffRef, product });
    sendRedirect(ctx.res, `${partner.returnUrl}?ref=${encodeURIComponent(handoffRef)}`);
  };
}
