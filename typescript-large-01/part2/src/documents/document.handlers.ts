import { HttpError, requireString, sendJson, sendRedirect } from '../http/respond';
import { RequestContext } from '../http/router';
import { errorMessage, log } from '../support/log';
import { DedupeIndex } from './dedupe-index';
import { inspectHeader, InvalidDocumentError } from './document-inspector';
import { DocumentPresigner } from './presigner';
import { TextExtractor } from './text-extractor';
import { StoredUpload, UploadStore } from './upload-store';

const MAX_UPLOAD_BYTES = 8 * 1024 * 1024;

interface DocumentRecord {
  ownerId: string;
  objectKey: string;
  upload: StoredUpload;
  searchableText: string | null;
}

export class DocumentHandlers {
  private readonly documents = new Map<string, DocumentRecord>();

  constructor(
    private readonly store: UploadStore,
    private readonly dedupe: DedupeIndex,
    private readonly extractor: TextExtractor,
    private readonly presigner: DocumentPresigner,
  ) {}

  upload = async (ctx: RequestContext): Promise<void> => {
    const ownerId = ctx.userId as string;
    const fileName = requireString(ctx.body, 'fileName');
    const data = Buffer.from(requireString(ctx.body, 'contentBase64'), 'base64');
    if (data.length === 0 || data.length > MAX_UPLOAD_BYTES) {
      throw new HttpError(400, 'document must be between 1 byte and 8 MiB');
    }
    const duplicate = await this.dedupe.findDuplicate(ownerId, data);
    if (duplicate) {
      sendJson(ctx.res, 200, { documentId: duplicate.uploadId, duplicate: true });
      return;
    }

    const stored = await this.store.save(ownerId, fileName, data);
    try {
      await inspectHeader(stored.path);
    } catch (err) {
      if (err instanceof InvalidDocumentError) throw new HttpError(422, err.message);
      throw err;
    }
    this.dedupe.record(stored, data);
    const record: DocumentRecord = { ownerId, objectKey: `borrower-docs/${stored.uploadId}.pdf`, upload: stored, searchableText: null };
    this.documents.set(stored.uploadId, record);
    this.extractor
      .extract(stored)
      .then((extracted) => (record.searchableText = extracted.text))
      .catch((err) => log.warn('document text extraction failed', { documentId: stored.uploadId, error: errorMessage(err) }));
    log.info('document uploaded', { documentId: stored.uploadId, userId: ownerId, sizeBytes: stored.sizeBytes });
    sendJson(ctx.res, 201, { documentId: stored.uploadId, duplicate: false });
  };

  download = async (ctx: RequestContext): Promise<void> => {
    const record = this.documents.get(ctx.params.documentId);
    if (!record || record.ownerId !== ctx.userId) {
      throw new HttpError(404, 'document not found');
    }
    const download = this.presigner.presign(record.objectKey);
    log.info('document download issued', { documentId: record.upload.uploadId, userId: ctx.userId });
    sendRedirect(ctx.res, download.url);
  };
}
