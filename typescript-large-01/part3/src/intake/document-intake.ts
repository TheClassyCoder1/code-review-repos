import { randomUUID } from 'crypto';
import { stat } from 'fs/promises';
import { extname } from 'path';
import { ValidationError } from '../util/errors';
import { Logger } from '../util/logger';
import { parseIncomeDocument, ParsedIncomeDocument } from './document-parser';

const ALLOWED_EXTENSIONS = new Set(['.csv', '.txt']);

/**
 * Entry point for uploaded income documents. Size and type are enforced here,
 * before the file is handed to the parser, so the parser only ever sees files
 * no larger than maxUploadBytes.
 */
export class DocumentIntake {
  constructor(
    private readonly maxUploadBytes: number,
    private readonly log: Logger,
  ) {}

  async accept(loanId: string, uploadedPath: string): Promise<ParsedIncomeDocument> {
    if (!ALLOWED_EXTENSIONS.has(extname(uploadedPath).toLowerCase())) {
      throw new ValidationError(`unsupported document type for ${loanId}`);
    }
    const info = await stat(uploadedPath);
    if (!info.isFile()) {
      throw new ValidationError(`upload for ${loanId} is not a regular file`);
    }
    if (info.size > this.maxUploadBytes) {
      throw new ValidationError(`upload for ${loanId} exceeds ${this.maxUploadBytes} bytes`);
    }
    const documentId = randomUUID();
    const parsed = await parseIncomeDocument(documentId, uploadedPath);
    this.log.info('income document accepted', { loanId, documentId, rows: parsed.lines.length });
    return parsed;
  }
}
