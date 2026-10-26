import { open } from 'fs/promises';

const HEADER_BYTES = 1024;
const PDF_MAGIC = Buffer.from('%PDF-');

export interface DocumentHeader {
  pdfVersion: string;
  encrypted: boolean;
}

export class InvalidDocumentError extends Error {}

/**
 * Sniffs the first kilobyte of an upload to make sure it is really a PDF and
 * that it is not password protected (underwriting cannot open those).
 */
export async function inspectHeader(filePath: string): Promise<DocumentHeader> {
  const handle = await open(filePath, 'r');
  const buf = Buffer.alloc(HEADER_BYTES);
  const { bytesRead } = await handle.read(buf, 0, HEADER_BYTES, 0);
  const head = buf.subarray(0, bytesRead);
  if (bytesRead < PDF_MAGIC.length || !head.subarray(0, PDF_MAGIC.length).equals(PDF_MAGIC)) {
    throw new InvalidDocumentError('upload is not a PDF document');
  }
  const ascii = head.toString('latin1');
  const version = /^%PDF-(\d\.\d)/.exec(ascii)?.[1] ?? 'unknown';
  const encrypted = ascii.includes('/Encrypt');
  if (encrypted) {
    throw new InvalidDocumentError('password-protected PDFs are not accepted');
  }
  await handle.close();
  return { pdfVersion: version, encrypted };
}
