import { Body, Controller, Get, Header, Param, Patch, Post, Query, Req, Res } from '@nestjs/common';
import type { Request, Response } from 'express';
import { randomUUID } from 'crypto';
import { PortalUser } from '../common/admin.guard';
import { limitStream } from '../common/limited-stream';
import { parseInvoiceRef, pickStatementPatch, BillingAccount, Statement } from './billing.dto';
import { BILLING_SESSION_COOKIE, MAX_UPLOAD_BYTES, SESSION_COOKIE_OPTIONS } from './billing.constants';
import { StatementsService } from './statements.service';
import { BillingAccountService } from './billing-account.service';
import { ReportExportService } from './report-export.service';
import { UsageImportService } from './usage-import.service';
import { UsageMeterService } from './usage-meter.service';
import { renderPageShell, renderReceiptBanner, renderStatementHtml } from './statement-renderer';

type AuthedRequest = Request & { user: PortalUser };

@Controller('billing')
export class StatementsController {
  constructor(
    private readonly statements: StatementsService,
    private readonly accounts: BillingAccountService,
    private readonly exporter: ReportExportService,
    private readonly usageImport: UsageImportService,
    private readonly usageMeter: UsageMeterService,
  ) {}

  @Post('session')
  startSession(@Req() req: AuthedRequest, @Res({ passthrough: true }) res: Response): { expiresIn: number } {
    const sid = randomUUID();
    const maxAge = 30 * 60 * 1000;
    res.cookie(BILLING_SESSION_COOKIE, sid, { ...SESSION_COOKIE_OPTIONS, maxAge });
    res.setHeader('Cache-Control', 'no-store');
    return { expiresIn: maxAge / 1000 };
  }

  @Get('statements')
  list(@Req() req: AuthedRequest, @Query('page') page?: string) {
    return this.statements.listStatements(req.user.accountId, Number(page ?? 1));
  }

  @Get('statements/:id/view')
  @Header('Content-Type', 'text/html; charset=utf-8')
  view(@Req() req: AuthedRequest, @Param('id') id: string): string {
    const statement = this.statements.findStatement(req.user.accountId, id);
    const account = this.accounts.getAccount(req.user.accountId);
    return renderPageShell(`Statement ${statement.period}`, renderStatementHtml(statement, account.displayName));
  }

  @Get('receipts/:ref')
  @Header('Content-Type', 'text/html; charset=utf-8')
  receipt(@Req() req: AuthedRequest, @Param('ref') rawRef: string, @Query('paid') paid: string): string {
    const reference = parseInvoiceRef(rawRef);
    const paidCents = Number.parseInt(paid ?? '0', 10) || 0;
    return renderPageShell('Payment receipt', renderReceiptBanner(reference, paidCents));
  }

  @Patch('statements/:id')
  update(@Req() req: AuthedRequest, @Param('id') id: string, @Body() body: Record<string, unknown>): Statement {
    return this.statements.updateStatement(req.user.accountId, id, pickStatementPatch(body));
  }

  @Patch('account')
  updateAccount(@Req() req: AuthedRequest, @Body() body: Partial<BillingAccount>): BillingAccount {
    return this.accounts.updateProfile(req.user.accountId, body);
  }

  @Get('statements.csv')
  @Header('Content-Type', 'text/csv; charset=utf-8')
  exportCsv(@Req() req: AuthedRequest, @Query('derived') derived?: string): string {
    const { items } = this.statements.listStatements(req.user.accountId, 1);
    return this.exporter.buildCsv(this.exporter.toRows(items), derived);
  }

  @Post('usage/import')
  async importUsage(@Req() req: AuthedRequest): Promise<{ imported: number; total: number }> {
    const raw = await this.usageImport.collectUpload(req.pipe(limitStream(MAX_UPLOAD_BYTES)));
    const records = this.usageImport.parseUsageCsv(raw.toString('utf8'));
    let total = this.usageMeter.currentUsage(req.user.accountId);
    for (const record of records.filter((r) => r.accountId === req.user.accountId)) {
      total = await this.usageMeter.recordUsage(record.accountId, record.units);
    }
    return { imported: records.length, total };
  }
}
