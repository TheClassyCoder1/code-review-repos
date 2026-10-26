import { exec } from 'child_process';
import { promisify } from 'util';
import { log } from '../support/log';
import { StoredUpload } from './upload-store';

const execAsync = promisify(exec);

const MAX_PAGES = 20;
const EXTRACT_TIMEOUT_MS = 15_000;

export interface ExtractedText {
  uploadId: string;
  text: string;
  pages: number;
}

/**
 * Pulls the text layer out of uploaded income/ID documents with poppler's
 * pdftotext so underwriting can search them.
 */
export class TextExtractor {
  async extract(upload: StoredUpload): Promise<ExtractedText> {
    const { stdout } = await execAsync(`pdftotext -layout -l ${MAX_PAGES} ${upload.path} -`, {
      timeout: EXTRACT_TIMEOUT_MS,
      maxBuffer: 8 * 1024 * 1024,
    });
    const pages = stdout.split('\f').filter((p) => p.trim().length > 0).length;
    log.info('document text extracted', { uploadId: upload.uploadId, pages });
    return { uploadId: upload.uploadId, text: stdout, pages };
  }
}
