import { createHmac, timingSafeEqual } from 'crypto';
import { requireSecret } from './secret-guard';

/**
 * HMAC key for signing outbound servicing webhooks (payment posted, payoff completed)
 * delivered to partner lenders. Resolved once at startup.
 */
export function loadWebhookSigningKey(env: NodeJS.ProcessEnv = process.env): string {
  return requireSecret('SERVICING_WEBHOOK_SIGNING_KEY', { testDefault: 'test-only-webhook-signing-key' }, env);
}

export class WebhookSigner {
  constructor(private readonly key: string) {}

  /** `t=<unix>,v1=<hex>` header value over `<timestamp>.<body>`. */
  sign(body: string, timestampSeconds: number): string {
    const mac = createHmac('sha256', this.key).update(`${timestampSeconds}.${body}`).digest('hex');
    return `t=${timestampSeconds},v1=${mac}`;
  }

  verify(body: string, header: string, nowSeconds: number, toleranceSeconds = 300): boolean {
    const fields = new Map(
      header.split(',').map((kv) => {
        const idx = kv.indexOf('=');
        return [kv.slice(0, idx), kv.slice(idx + 1)] as [string, string];
      }),
    );
    const timestamp = Number(fields.get('t'));
    const provided = fields.get('v1');
    if (!Number.isFinite(timestamp) || !provided || Math.abs(nowSeconds - timestamp) > toleranceSeconds) {
      return false;
    }
    const expected = createHmac('sha256', this.key).update(`${timestamp}.${body}`).digest('hex');
    const a = Buffer.from(provided, 'utf8');
    const b = Buffer.from(expected, 'utf8');
    return a.length === b.length && timingSafeEqual(a, b);
  }
}
