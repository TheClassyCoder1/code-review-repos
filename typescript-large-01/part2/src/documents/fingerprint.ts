import { createHash } from 'crypto';

/**
 * Cheap content fingerprint used to spot borrowers re-uploading the same
 * payslip or bank statement several times.
 */
export function contentFingerprint(data: Buffer): string {
  return createHash('md5').update(data).digest('hex');
}
