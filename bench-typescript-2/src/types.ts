export interface Account {
  id: string;
  name: string;
}

export interface PriceTier {
  upTo: number | null;
  unitAmountCents: number;
}

export interface Plan {
  id: string;
  account_id: string;
  code: string;
  name: string;
  interval: 'month' | 'year';
  amount_cents: number;
  currency: string;
  tiers: PriceTier[];
}

export interface Subscription {
  id: string;
  account_id: string;
  customer_id: string;
  plan_id: string;
  status: 'trialing' | 'active' | 'paused' | 'past_due' | 'canceled';
  seats: number;
  current_period_start: Date;
  current_period_end: Date;
}

export interface Invoice {
  id: string;
  account_id: string;
  subscription_id: string;
  status: 'draft' | 'open' | 'paid' | 'void' | 'refunded';
  total_cents: number;
  currency: string;
  payment_id: string | null;
  due_at: Date;
}

export interface InvoiceLine {
  description: string;
  quantity: number;
  amount_cents: number;
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      account?: Account;
    }
  }
}
