import { GatewaySettings } from '../config/settings';
import { formatMinor } from '../domain/money';
import { DisbursementRecord } from '../domain/types';
import { Logger } from '../util/logger';
import { retryAsync } from '../util/retry';
import { HttpTransport, TransportResponse } from './transport';

export type GatewayOutcome =
  | { kind: 'accepted'; gatewayReference: string }
  | { kind: 'rejected'; reasonCode: string }
  | { kind: 'duplicate'; gatewayReference: string };

export class GatewayUnavailableError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
    this.name = 'GatewayUnavailableError';
  }
}

const REFERENCE_PATTERN = /^[A-Z0-9-]{8,64}$/;

/**
 * Interprets a payout gateway response. 2xx carries the gateway reference,
 * 409 means the idempotency key was already used (the original reference is
 * returned), 422 is a business rejection, and 429/5xx are retryable.
 */
export function parseGatewayResponse(response: TransportResponse): GatewayOutcome {
  if (response.status === 429 || response.status >= 500) {
    throw new GatewayUnavailableError(response.status, `gateway returned ${response.status}`);
  }

  let payload: { reference?: unknown; reasonCode?: unknown };
  try {
    payload = JSON.parse(response.body) as typeof payload;
  } catch {
    throw new Error(`gateway returned a non-JSON body with status ${response.status}`);
  }

  if (response.status === 422) {
    const reasonCode = typeof payload.reasonCode === 'string' ? payload.reasonCode : 'UNSPECIFIED';
    return { kind: 'rejected', reasonCode };
  }
  if (response.status !== 409 && (response.status < 200 || response.status > 299)) {
    throw new Error(`unexpected gateway status ${response.status}`);
  }
  if (typeof payload.reference !== 'string' || !REFERENCE_PATTERN.test(payload.reference)) {
    throw new Error('gateway response is missing a valid reference');
  }
  return response.status === 409
    ? { kind: 'duplicate', gatewayReference: payload.reference }
    : { kind: 'accepted', gatewayReference: payload.reference };
}

function isRetryable(error: unknown): boolean {
  if (error instanceof GatewayUnavailableError) {
    return true;
  }
  const code = (error as NodeJS.ErrnoException | undefined)?.code;
  const name = (error as Error | undefined)?.name;
  return name === 'TimeoutError' || name === 'AbortError' || code === 'ECONNRESET' || code === 'ECONNREFUSED';
}

/**
 * Submits payouts to the disbursement gateway. Every attempt gets its own
 * deadline from settings, and the disbursement id doubles as the idempotency
 * key so a retried submit can never pay out twice.
 */
export class GatewayClient {
  constructor(
    private readonly transport: HttpTransport,
    private readonly settings: GatewaySettings,
    private readonly apiKey: string,
    private readonly logger: Logger,
  ) {}

  async submitPayout(record: DisbursementRecord): Promise<GatewayOutcome> {
    const url = new URL('/v1/payouts', this.settings.baseUrl);
    const body = JSON.stringify({
      idempotencyKey: record.disbursementId,
      loanId: record.loanId,
      destination: record.accountRef,
      amount: formatMinor(record.amountMinor, record.currency),
      currency: record.currency,
    });

    return retryAsync(
      async (attempt) => {
        this.logger.log('info', 'gateway.submit', { disbursementId: record.disbursementId, attempt });
        const response = await this.transport.send(
          url,
          {
            method: 'POST',
            headers: {
              'content-type': 'application/json',
              'content-length': Buffer.byteLength(body),
              'x-api-key': this.apiKey,
            },
            signal: AbortSignal.timeout(this.settings.requestTimeoutMs),
          },
          body,
        );
        return parseGatewayResponse(response);
      },
      { baseDelayMs: 200, maxDelayMs: 4_000, maxAttempts: this.settings.maxAttempts },
      isRetryable,
    );
  }
}
