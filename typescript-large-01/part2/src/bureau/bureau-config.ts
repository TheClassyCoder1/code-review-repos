export type BureauMode = 'live' | 'sandbox';

export interface BureauConfig {
  mode: BureauMode;
  baseUrl: string;
  apiKey: string;
  timeoutMs: number;
}

const LOOPBACK_HOSTS = new Set(['localhost', '127.0.0.1', '::1', '[::1]']);

/**
 * Loads credit-bureau settings. Sandbox mode targets the bureau's stub
 * container, which serves a self-signed certificate; it is refused outright in
 * production and is only accepted when the stub runs on the loopback interface.
 */
export function loadBureauConfig(env: NodeJS.ProcessEnv = process.env): BureauConfig {
  const mode: BureauMode = env.BUREAU_MODE === 'sandbox' ? 'sandbox' : 'live';
  const baseUrl = env.BUREAU_BASE_URL ?? (mode === 'sandbox' ? 'https://localhost:9443' : 'https://api.credit-bureau.test');
  const timeoutMs = Number(env.BUREAU_TIMEOUT_MS ?? 4000);

  if (mode === 'sandbox') {
    if (env.NODE_ENV === 'production') {
      throw new Error('BUREAU_MODE=sandbox is not permitted when NODE_ENV=production');
    }
    const host = new URL(baseUrl).hostname;
    if (!LOOPBACK_HOSTS.has(host)) {
      throw new Error('sandbox bureau must run on the loopback interface');
    }
  }

  return {
    mode,
    baseUrl,
    apiKey: env.BUREAU_API_KEY ?? 'bureau-sandbox-key-local',
    timeoutMs: Number.isFinite(timeoutMs) && timeoutMs > 0 ? timeoutMs : 4000,
  };
}
