import http from 'http';
import https from 'https';

export interface SimpleResponse {
  status: number;
  body: string;
}

export interface PostOptions {
  headers?: Record<string, string>;
  agent?: https.Agent;
  timeoutMs?: number;
}

export function postRaw(url: string, payload: string | null, options: PostOptions = {}): Promise<SimpleResponse> {
  const target = new URL(url);
  const transport = target.protocol === 'https:' ? https : http;
  return new Promise((resolve, reject) => {
    const req = transport.request(
      target,
      {
        method: 'POST',
        headers: options.headers,
        agent: target.protocol === 'https:' ? options.agent : undefined,
        timeout: options.timeoutMs ?? 10_000,
      },
      (res) => {
        const chunks: Buffer[] = [];
        res.on('data', (c: Buffer) => chunks.push(c));
        res.on('end', () => resolve({ status: res.statusCode ?? 0, body: Buffer.concat(chunks).toString('utf8') }));
        res.on('error', reject);
      },
    );
    req.on('timeout', () => req.destroy(new Error(`request to ${target.host} timed out`)));
    req.on('error', reject);
    if (payload !== null) {
      req.write(payload);
    }
    req.end();
  });
}

export function getRaw(url: string, timeoutMs = 10_000): Promise<SimpleResponse> {
  const target = new URL(url);
  const transport = target.protocol === 'https:' ? https : http;
  return new Promise((resolve, reject) => {
    const req = transport.get(target, { timeout: timeoutMs }, (res) => {
      const chunks: Buffer[] = [];
      res.on('data', (c: Buffer) => chunks.push(c));
      res.on('end', () => resolve({ status: res.statusCode ?? 0, body: Buffer.concat(chunks).toString('utf8') }));
      res.on('error', reject);
    });
    req.on('timeout', () => req.destroy(new Error(`request to ${target.host} timed out`)));
    req.on('error', reject);
  });
}
