export interface BackoffPolicy {
  baseDelayMs: number;
  maxDelayMs: number;
  maxAttempts: number;
}

export class RetryExhaustedError extends Error {
  constructor(readonly attempts: number, readonly lastError: unknown) {
    super(`gave up after ${attempts} attempts`);
    this.name = 'RetryExhaustedError';
  }
}

/**
 * "Full jitter" exponential backoff: a uniform delay in [0, min(max, base * 2^n)).
 * `random` is injectable so schedules are reproducible in tests.
 */
export function computeBackoffDelay(
  attempt: number,
  policy: BackoffPolicy,
  random: () => number = Math.random,
): number {
  if (!Number.isInteger(attempt) || attempt < 1) {
    throw new RangeError('attempt numbers start at 1');
  }
  if (policy.baseDelayMs <= 0 || policy.maxDelayMs < policy.baseDelayMs) {
    throw new RangeError('backoff policy needs 0 < base <= max');
  }
  // Cap the exponent before shifting so very high attempt counts cannot overflow to Infinity.
  const exponent = Math.min(attempt - 1, 30);
  const ceiling = Math.min(policy.maxDelayMs, policy.baseDelayMs * 2 ** exponent);
  const sample = random();
  const clamped = sample < 0 ? 0 : sample >= 1 ? 0.999999 : sample;
  return Math.floor(clamped * ceiling);
}

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

/**
 * Runs `operation` until it succeeds, `isRetryable` rejects the error, or the
 * attempt budget is spent. Non-retryable errors propagate unchanged.
 */
export async function retryAsync<T>(
  operation: (attempt: number) => Promise<T>,
  policy: BackoffPolicy,
  isRetryable: (error: unknown) => boolean,
  wait: (ms: number) => Promise<void> = sleep,
): Promise<T> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= policy.maxAttempts; attempt++) {
    try {
      return await operation(attempt);
    } catch (error) {
      if (!isRetryable(error)) {
        throw error;
      }
      lastError = error;
      if (attempt < policy.maxAttempts) {
        await wait(computeBackoffDelay(attempt, policy));
      }
    }
  }
  throw new RetryExhaustedError(policy.maxAttempts, lastError);
}
