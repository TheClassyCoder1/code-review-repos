import { randomUUID } from 'crypto';
import { mkdir, writeFile } from 'fs/promises';
import * as path from 'path';

export interface StoredUpload {
  uploadId: string;
  ownerId: string;
  /** Server-generated absolute path: `<uploadsDir>/<uuid>.pdf`. */
  path: string;
  /** Name the borrower's browser sent; metadata only, never used on disk. */
  originalName: string;
  sizeBytes: number;
}

/**
 * Persists borrower uploads. On-disk names are generated here from a UUID, so
 * downstream tooling only ever sees paths of the form `<uploadsDir>/<uuid>.pdf`.
 */
export class UploadStore {
  private readonly root: string;

  constructor(uploadsDir: string) {
    this.root = path.resolve(uploadsDir);
    if (/[^A-Za-z0-9_./-]/.test(this.root)) {
      throw new Error('uploads directory must be a plain path (letters, digits, _ . / -)');
    }
  }

  async save(ownerId: string, originalName: string, data: Buffer): Promise<StoredUpload> {
    await mkdir(this.root, { recursive: true });
    const uploadId = randomUUID();
    const filePath = path.join(this.root, `${uploadId}.pdf`);
    await writeFile(filePath, data, { mode: 0o600, flag: 'wx' });
    return { uploadId, ownerId, path: filePath, originalName: originalName.slice(0, 255), sizeBytes: data.length };
  }
}
