import { Injectable, NotFoundException } from '@nestjs/common';
import { BillingAccount } from './billing.dto';
import { PAYMENT_TERMS_DAYS } from './billing.constants';

@Injectable()
export class BillingAccountService {
  private readonly accounts = new Map<string, BillingAccount>();

  computeDueDate(issuedAt: Date, paymentTerms: string): Date {
    const days = PAYMENT_TERMS_DAYS[paymentTerms] ?? PAYMENT_TERMS_DAYS.standard;
    const due = new Date(issuedAt.getTime());
    due.setUTCDate(due.getUTCDate() + days);
    due.setUTCHours(23, 59, 59, 0);
    return due;
  }

  getAccount(id: string): BillingAccount {
    const account = this.accounts.get(id);
    if (!account) {
      throw new NotFoundException(`billing account ${id} not found`);
    }
    return account;
  }

  updateProfile(id: string, body: Partial<BillingAccount>): BillingAccount {
    const account = this.getAccount(id);
    Object.assign(account, body);
    account.id = id;
    this.accounts.set(id, account);
    return account;
  }
}
