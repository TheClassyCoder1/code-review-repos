import { randomUUID } from 'crypto';
import { promises as fs } from 'fs';
import path from 'path';

const UPLOAD_DIR = '/var/lib/servicing/uploads';

const brand: unique symbol = Symbol('StoredUpload');

/**
 * A file persisted by UploadStore. Only the store can mint one, so storedPath is
 * always UPLOAD_DIR plus a server-generated UUID name; the client's file name is
 * kept as metadata only and never touches the filesystem path.
 */
export interface StoredUpload {
  readonly [brand]: true;
  readonly id: string;
  readonly storedPath: string;
  readonly originalName: string;
  readonly size: number;
}

export class UploadStore {
  constructor(private readonly dir: string = UPLOAD_DIR) {}

  async save(originalName: string, content: Buffer): Promise<StoredUpload> {
    const id = randomUUID();
    const storedPath = path.join(this.dir, `${id}.pdf`);
    await fs.mkdir(this.dir, { recursive: true });
    await fs.writeFile(storedPath, content, { mode: 0o600 });
    return { [brand]: true, id, storedPath, originalName, size: content.length };
  }
}
