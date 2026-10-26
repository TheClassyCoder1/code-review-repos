import { createHmac } from 'crypto';

const OBJECT_KEY = /^[a-z0-9-]+\/[0-9a-f-]{36}\.pdf$/;

/**
 * Issues short-lived download URLs for the document bucket. The scheme and
 * host come exclusively from configuration; only a validated object key is
 * appended, so the result always points at the document store.
 */
export class DocumentPresigner {
  private readonly base: URL;

  constructor(
    storageHost: string,
    private readonly signingKey: string,
    private readonly ttlSeconds = 300,
  ) {
    this.base = new URL(`https://${storageHost}/`);
    if (this.base.hostname !== storageHost) throw new Error('storage host must be a bare hostname');
  }

  presign(objectKey: string, now = Date.now()): { url: string; expiresAt: number } {
    if (!OBJECT_KEY.test(objectKey)) throw new Error('invalid object key');
    const expiresAt = Math.floor(now / 1000) + this.ttlSeconds;
    const url = new URL(objectKey, this.base);
    url.searchParams.set('expires', String(expiresAt));
    url.searchParams.set('sig', createHmac('sha256', this.signingKey).update(`${objectKey}\n${expiresAt}`).digest('hex'));
    return { url: url.toString(), expiresAt };
  }
}
