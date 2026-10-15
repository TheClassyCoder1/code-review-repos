import { BadRequestException } from '@nestjs/common';

export type StatementStatus = 'draft' | 'issued' | 'paid' | 'void';

export interface StatementLine {
  description: string;
  amountCents: number;
  quantity: number;
}

export interface Statement {
  id: string;
  accountId: string;
  reference: string;
  period: string;
  status: StatementStatus;
  memo: string;
  customerNote: string;
  lines: StatementLine[];
  issuedAt?: string;
}

export interface BillingAccount {
  id: string;
  displayName: string;
  contactEmail: string;
  paymentTerms: string;
  role: 'member' | 'owner' | 'billing_admin';
  creditLimitCents: number;
}

export interface StatementPatch {
  memo?: string;
  customerNote?: string;
}

const INVOICE_REF_PATTERN = /^[A-Z0-9-]{6,20}$/;

const PERIOD_PATTERN = /^(\d{4})-(0[1-9]|1[0-2])$/;

export function parseInvoiceRef(raw: unknown): string {
  if (typeof raw !== 'string' || !INVOICE_REF_PATTERN.test(raw)) {
    throw new BadRequestException('invalid invoice reference');
  }
  return raw;
}

export function pickStatementPatch(body: Record<string, unknown>): StatementPatch {
  const patch: StatementPatch = {};
  if (typeof body.memo === 'string') {
    patch.memo = body.memo.slice(0, 500);
  }
  if (typeof body.customerNote === 'string') {
    patch.customerNote = body.customerNote.slice(0, 500);
  }
  return patch;
}

export function parsePeriod(raw: string): { year: number; month: number } {
  const match = PERIOD_PATTERN.exec(raw);
  if (!match) {
    throw new BadRequestException('period must be YYYY-MM');
  }
  return { year: Number(match[1]), month: Number(match[2]) };
}
