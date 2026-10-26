import * as http from 'http';
import * as https from 'https';

export interface TransportResponse {
  status: number;
  headers: http.IncomingHttpHeaders;
  body: string;
}

const MAX_BODY_BYTES = 1_048_576;

/**
 * Thin promise wrapper over node's http/https client. It owns body buffering
 * and size limits only; request policy (headers, retries, deadlines) is decided
 * by the client that builds `options`.
 */
export class HttpTransport {
  send(url: URL, options: http.RequestOptions, body?: string): Promise<TransportResponse> {
    const client = url.protocol === 'https:' ? https : http;
    return new Promise<TransportResponse>((resolve, reject) => {
      const req = client.request(url, options, (res) => {
        const chunks: Buffer[] = [];
        let received = 0;
        res.on('data', (chunk: Buffer) => {
          received += chunk.length;
          if (received > MAX_BODY_BYTES) {
            req.destroy(new Error('gateway response exceeded 1 MiB'));
            return;
          }
          chunks.push(chunk);
        });
        res.on('end', () => {
          resolve({
            status: res.statusCode ?? 0,
            headers: res.headers,
            body: Buffer.concat(chunks).toString('utf8'),
          });
        });
        res.on('error', reject);
      });
      req.on('error', reject);
      if (body !== undefined) {
        req.write(body);
      }
      req.end();
    });
  }
}
