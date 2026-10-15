import { config } from '../config';
import { Seller } from './sellerService';

export interface PayoutLine {
  payoutId: string;
  amountCents: number;
  currency: string;
  paidAt: Date;
}

export function renderWelcomeEmail(seller: Pick<Seller, 'id' | 'displayName'>): { subject: string; html: string } {
  const dashboardUrl = `${config.publicBaseUrl}/sellers/${encodeURIComponent(seller.id)}/dashboard`;
  const html = `<p>Hi ${seller.displayName},</p>
<p>Your seller account has been created. Upload your KYC documents to start selling.</p>
<p><a href="${dashboardUrl}">Open your dashboard</a></p>`;
  return { subject: `Welcome to the marketplace, ${seller.displayName}`, html };
}

function formatMoney(amountCents: number, currency: string): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amountCents / 100);
}

const DATE_FORMAT: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'short', day: '2-digit' };

export function formatPayoutSummary(lines: PayoutLine[]): string {
  if (lines.length === 0) {
    return 'No payouts were sent in this period.';
  }
  const totals = new Map<string, number>();
  const body = lines.map((line) => {
    totals.set(line.currency, (totals.get(line.currency) ?? 0) + line.amountCents);
    return `${line.paidAt.toLocaleDateString('en-US', DATE_FORMAT)}  ${line.payoutId}  ${formatMoney(line.amountCents, line.currency)}`;
  });
  const footer = [...totals.entries()].map(([currency, cents]) => `Total ${currency}: ${formatMoney(cents, currency)}`);
  return [...body, '', ...footer].join('\n');
}
