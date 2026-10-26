export type FetchLike = (input: string, init?: RequestInit) => Promise<Response>;

/**
 * Wraps the global fetch so every request carries a deadline. If the caller
 * already supplied a signal, both are honoured: whichever fires first aborts.
 */
export function timedFetch(timeoutMs: number, base: FetchLike = fetch): FetchLike {
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) {
    throw new RangeError(`timedFetch requires a positive timeout, got ${timeoutMs}`);
  }
  return (input, init = {}) => {
    const deadline = AbortSignal.timeout(timeoutMs);
    const signal = init.signal ? AbortSignal.any([init.signal, deadline]) : deadline;
    return base(input, { ...init, signal });
  };
}

export async function readJson<T>(res: Response, what: string): Promise<T> {
  const text = await res.text();
  if (text.length === 0) {
    throw new Error(`${what}: empty response body`);
  }
  try {
    return JSON.parse(text) as T;
  } catch (err) {
    throw new Error(`${what}: response is not JSON`);
  }
}
