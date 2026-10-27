import { exec } from 'child_process';
import { StoredUpload } from './uploadStore';

/** Extracts the text layer of an uploaded PDF with poppler's pdftotext. */
export function extractPdfText(upload: StoredUpload): Promise<string> {
  return new Promise((resolve, reject) => {
    exec(`pdftotext -layout ${upload.storedPath} -`, { timeout: 30_000, maxBuffer: 8 * 1024 * 1024 }, (err, stdout) => {
      if (err) {
        reject(err);
        return;
      }
      resolve(stdout);
    });
  });
}
