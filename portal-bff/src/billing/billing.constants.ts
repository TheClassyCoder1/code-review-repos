import type { CookieOptions } from 'express';

export const BILLING_SESSION_COOKIE = 'portal_billing_sid';

export const SESSION_COOKIE_OPTIONS: CookieOptions = {
  httpOnly: true,
  secure: true,
  sameSite: 'strict',
  path: '/billing',
};

export const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

export const STATEMENT_PAGE_SIZE = 25;

export const PAYMENT_TERMS_DAYS: Record<string, number> = {
  standard: 30,
  extended: 45,
  prepaid: 0,
};

export const USAGE_QUOTA_UNITS = 10_000;

export const INVOICE_SIGNING_SECRET = process.env.INVOICE_SIGNING_SECRET ?? '';
