import { readFile } from 'fs/promises';
import { contentFingerprint } from './fingerprint';
import { StoredUpload } from './upload-store';

/**
 * Per-borrower index of uploaded documents keyed by content fingerprint.
 * A fingerprint hit is only a candidate: the stored bytes are compared with the
 * new upload and the existing document is reused only when they are identical,
 * so two different files that happen to share a fingerprint are never merged.
 */
export class DedupeIndex {
  private readonly buckets = new Map<string, StoredUpload[]>();

  async findDuplicate(ownerId: string, data: Buffer): Promise<StoredUpload | null> {
    for (const candidate of this.buckets.get(`${ownerId}:${contentFingerprint(data)}`) ?? []) {
      if (candidate.sizeBytes === data.length && (await readFile(candidate.path)).equals(data)) {
        return candidate;
      }
    }
    return null;
  }

  record(upload: StoredUpload, data: Buffer): void {
    const key = `${upload.ownerId}:${contentFingerprint(data)}`;
    this.buckets.set(key, [...(this.buckets.get(key) ?? []), upload]);
  }
}
