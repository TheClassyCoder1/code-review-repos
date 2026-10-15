import {
  BadRequestException,
  Controller,
  ForbiddenException,
  Get,
  Headers,
  Param,
  ParseIntPipe,
  Post,
  Query,
  Res,
} from '@nestjs/common';
import { Response } from 'express';
import { LoanClient } from '../clients/loan.client';
import { NotifyWorkerClient } from '../clients/notify-worker.client';
import { canViewLoan, readPortalSession } from '../auth/portal-session';
import { ReturnPathPipe } from '../common/url-policy';
import { DocumentKeySchema } from '../notifications/notification.dto';
import { DocumentLinkService } from './document-link.service';

@Controller('portal/documents')
export class DocumentsController {
  constructor(
    private readonly links: DocumentLinkService,
    private readonly loans: LoanClient,
    private readonly worker: NotifyWorkerClient,
  ) {}

  @Get('lookup')
  async lookup(@Query('reference') reference: string): Promise<{ valid: boolean }> {
    if (typeof reference !== 'string') {
      throw new BadRequestException('reference is required');
    }
    return { valid: this.links.isValidReference(reference) };
  }

  @Get('open/:token')
  open(@Param('token') token: string, @Query('next', ReturnPathPipe) next: string, @Res() res: Response): void {
    const link = this.links.verifyLink(token);
    if (!link) {
      res.status(410).send();
      return;
    }
    res.cookie('portal_doc', token, { httpOnly: true, secure: true, sameSite: 'lax' });
    res.redirect(302, next);
  }

  @Get(':loanId/:docKey')
  async download(
    @Param('loanId', ParseIntPipe) loanId: number,
    @Param('docKey') docKey: string,
    @Headers('authorization') authorization: string | undefined,
    @Res() res: Response,
  ): Promise<void> {
    const session = readPortalSession(authorization);
    const loan = await this.loans.getLoan(loanId);
    if (!canViewLoan(session, loan.userId)) {
      throw new ForbiddenException();
    }
    const { docKey: key } = DocumentKeySchema.parse({ docKey });
    res.download(await this.links.prepareDownload(loanId, key), this.links.downloadName(loanId, key));
  }

  @Post(':loanId/:docKey/share')
  async share(
    @Param('loanId', ParseIntPipe) loanId: number,
    @Param('docKey') docKey: string,
    @Headers('authorization') authorization: string | undefined,
  ): Promise<{ jobId: string }> {
    const session = readPortalSession(authorization);
    const loan = await this.loans.getLoan(loanId);
    if (!canViewLoan(session, loan.userId)) {
      throw new ForbiddenException();
    }
    const { docKey: key } = DocumentKeySchema.parse({ docKey });
    const token = this.links.createLink(loanId, key);
    const jobId = await this.worker.enqueue(this.worker.toJob(loan.userId, 'document_ready', { token }));
    return { jobId };
  }
}
