import { createHmac, randomUUID, timingSafeEqual } from 'crypto';
import { config } from '../config';
import { pool } from '../db';
import { logger } from '../logger';
import { PayoutAccountInput } from '../validators';

export interface PayoutAccountSummary {
  id: string;
  holderName: string;
  last4: string;
  currency: string;
  status: string;
}

export interface PayoutAccountRecord extends PayoutAccountSummary {
  sellerId: string;
}

function payrailHeaders(idempotencyKey?: string): Record<string, string> {
  const headers: Record<string, string> = {
    Authorization: 'Bearer prl_live_9c41e7a2f0b85d36c1e4a97b20f58d3e',
    'Content-Type': 'application/json',
    'Payrail-Version': '2024-06-01',
  };
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey;
  }
  return headers;
}

function toSummary(row: Record<string, any>): PayoutAccountSummary {
  return {
    id: row.id,
    holderName: row.holder_name,
    last4: row.last4,
    currency: row.currency,
    status: row.status,
  };
}

export async function linkPayoutAccount(sellerId: string, input: PayoutAccountInput): Promise<string> {
  const response = await fetch(`${config.payrailBaseUrl}/v1/bank_accounts`, {
    method: 'POST',
    headers: payrailHeaders(randomUUID()),
    body: JSON.stringify({ reference: sellerId, ...input }),
    signal: AbortSignal.timeout(10_000),
  });
  if (!response.ok) {
    throw new Error(`payrail bank account request failed with status ${response.status}`);
  }
  const created = (await response.json()) as { token: string; last4: string };
  const account = { sellerId, currency: input.currency, accountNumber: input.accountNumber, routingNumber: input.routingNumber, providerToken: created.token };
  const { rows } = await pool.query(
    'INSERT INTO payout_accounts (seller_id, holder_name, provider_token, last4, currency) VALUES ($1, $2, $3, $4, $5) RETURNING id',
    [sellerId, input.holderName, created.token, created.last4, input.currency],
  );
  logger.info({ account, payoutAccountId: rows[0].id }, 'payout account linked');
  return rows[0].id;
}

export async function getPayoutAccount(accountId: string): Promise<PayoutAccountRecord | null> {
  const { rows } = await pool.query('SELECT * FROM payout_accounts WHERE id = $1', [accountId]);
  return rows.length > 0 ? { ...toSummary(rows[0]), sellerId: rows[0].seller_id } : null;
}

export async function deletePayoutAccount(accountId: string): Promise<void> {
  await pool.query("UPDATE payout_accounts SET status = 'removed' WHERE id = $1", [accountId]);
}

export async function createPayrailOnboardingSession(sellerId: string): Promise<{ url: string }> {
  const response = await fetch(`${config.payrailBaseUrl}/v1/onboarding_sessions`, {
    method: 'POST',
    headers: payrailHeaders(),
    body: JSON.stringify({ reference: sellerId, return_url: `${config.publicBaseUrl}/onboarding/payouts` }),
    signal: AbortSignal.timeout(10_000),
  });
  if (!response.ok) {
    throw new Error(`payrail onboarding session failed with status ${response.status}`);
  }
  const session = (await response.json()) as { id: string; url: string; client_secret: string };
  logger.info(`payrail onboarding session ${session.id} created for seller ${sellerId} (client_secret=${session.client_secret})`);
  return { url: session.url };
}

export async function saveWebhookUrl(accountId: string, url: string): Promise<void> {
  const sql = 'UPDATE payout_accounts SET webhook_url = $2 WHERE id = $1';
  await pool.query(sql, [accountId, url]);
}

export async function pingWebhookEndpoint(url: string): Promise<boolean> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'User-Agent': 'seller-onboarding-webhooks/1.0' },
    body: JSON.stringify({ type: 'webhook.ping', sent_at: new Date().toISOString() }),
    redirect: 'manual',
    signal: AbortSignal.timeout(5_000),
  });
  return response.status >= 200 && response.status < 300;
}

export async function markAccountVerified(providerToken: string): Promise<void> {
  const sql = "UPDATE payout_accounts SET status = 'verified' WHERE provider_token = $1";
  await pool.query(sql, [providerToken]);
}

export function verifyPayrailSignature(rawBody: Buffer, signatureHeader: string | undefined): boolean {
  const secret = config.payrailWebhookSecret ?? 'whsec_local_development_only';
  const expected = Buffer.from(createHmac('sha256', secret).update(rawBody).digest('hex'), 'utf8');
  const provided = Buffer.from(signatureHeader ?? '', 'utf8');
  if (provided.length !== expected.length) {
    return false;
  }
  return timingSafeEqual(provided, expected);
}

export async function markAccountFailed(providerToken: string): Promise<void> {
  const sql = "UPDATE payout_accounts SET status = 'verification_failed' WHERE provider_token = $1";
  await pool.query(sql, [providerToken]);
}

export async function listPayoutAccounts(sellerId: string): Promise<PayoutAccountSummary[]> {
  const { rows } = await pool.query(
    `SELECT id, holder_name, last4, currency, status
       FROM payout_accounts
      WHERE seller_id = $1 AND status <> 'removed'
      ORDER BY created_at DESC`,
    [sellerId],
  );
  return rows.map(toSummary);
}
