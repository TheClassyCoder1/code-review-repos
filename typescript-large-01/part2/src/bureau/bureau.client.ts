import * as https from 'https';
import { BureauConfig } from './bureau-config';

export interface BureauScore {
  applicantRef: string;
  score: number;
  model: string;
  pulledAt: string;
}

export interface BureauClient {
  fetchScore(applicantRef: string): Promise<BureauScore>;
}

/** Production bureau API over the platform trust store. */
export class LiveBureauClient implements BureauClient {
  constructor(private readonly config: BureauConfig) {}

  async fetchScore(applicantRef: string): Promise<BureauScore> {
    const res = await fetch(`${this.config.baseUrl}/v2/scores/${encodeURIComponent(applicantRef)}`, {
      headers: { 'X-Api-Key': this.config.apiKey, Accept: 'application/json' },
      signal: AbortSignal.timeout(this.config.timeoutMs),
    });
    if (!res.ok) throw new Error(`bureau returned ${res.status}`);
    return (await res.json()) as BureauScore;
  }
}

/** Local bureau stub container; it ships with a throwaway self-signed certificate. */
export class SandboxBureauClient implements BureauClient {
  private readonly agent = new https.Agent({ rejectUnauthorized: false, keepAlive: true });

  constructor(private readonly config: BureauConfig) {}

  fetchScore(applicantRef: string): Promise<BureauScore> {
    const url = new URL(`/v2/scores/${encodeURIComponent(applicantRef)}`, this.config.baseUrl);
    const options = { agent: this.agent, headers: { 'X-Api-Key': this.config.apiKey }, timeout: this.config.timeoutMs };
    return new Promise((resolve, reject) => {
      const req = https.get(url, options, (res) => {
        const chunks: Buffer[] = [];
        res.on('data', (c: Buffer) => chunks.push(c));
        res.on('end', () => {
          if ((res.statusCode ?? 500) >= 400) return reject(new Error(`sandbox bureau returned ${res.statusCode}`));
          try {
            resolve(JSON.parse(Buffer.concat(chunks).toString('utf8')) as BureauScore);
          } catch (err) {
            reject(err);
          }
        });
      });
      req.on('timeout', () => req.destroy(new Error('sandbox bureau timed out')));
      req.on('error', reject);
    });
  }
}

export function createBureauClient(config: BureauConfig): BureauClient {
  return config.mode === 'sandbox' ? new SandboxBureauClient(config) : new LiveBureauClient(config);
}
