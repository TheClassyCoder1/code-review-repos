export interface RetryOptions {
  attempts: number;
  baseDelayMs: number;
  maxDelayMs: number;
  /** Return false to stop retrying immediately (e.g. on 4xx responses). */
  isRetryable?: (error: unknown) => boolean;
  sleep?: (ms: number) => Promise<void>;
  random?: () => number;
}

const defaultSleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Runs `operation` up to `attempts` times with capped exponential backoff and full
 * jitter. Non-retryable errors and the error from the final attempt are rethrown as-is.
 */
export async function withRetry<T>(operation: (attempt: number) => Promise<T>, options: RetryOptions): Promise<T> {
  const { attempts, baseDelayMs, maxDelayMs } = options;
  if (!Number.isInteger(attempts) || attempts < 1) {
    throw new RangeError(`attempts must be a positive integer, got ${attempts}`);
  }
  const sleep = options.sleep ?? defaultSleep;
  const random = options.random ?? Math.random;
  const isRetryable = options.isRetryable ?? (() => true);

  let lastError: unknown;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await operation(attempt);
    } catch (error) {
      lastError = error;
      if (attempt === attempts || !isRetryable(error)) {
        throw error;
      }
      const ceiling = Math.min(maxDelayMs, baseDelayMs * 2 ** (attempt - 1));
      await sleep(Math.floor(random() * ceiling));
    }
  }
  throw lastError;
}

export class HttpStatusError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}

/** 5xx, 408 and 429 are transient; every other HTTP status is final. */
export function isTransientHttpError(error: unknown): boolean {
  if (error instanceof HttpStatusError) {
    return error.status >= 500 || error.status === 408 || error.status === 429;
  }
  return true;
}
