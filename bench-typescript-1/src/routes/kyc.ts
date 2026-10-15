import { NextFunction, Request, Response, Router } from 'express';
import multer from 'multer';
import { requireAuth } from '../middleware/auth';
import { deleteKycFile, generateThumbnail, readKycDocument, scanForMalware, storeKycFile } from '../services/kycStorage';
import { findOwnedKycDocument, recordKycDocument, removeKycDocumentRecord } from '../services/sellerService';
import { isStoredKycName } from '../validators';

export const kycRouter = Router();

const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 10 * 1024 * 1024, files: 1 } });

async function uploadKycDocumentHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  if (!req.file) {
    res.status(400).json({ error: 'file is required' });
    return;
  }
  try {
    const sellerId = req.user!.sellerId;
    const storedName = await storeKycFile(sellerId, req.file.buffer, req.file.mimetype);
    if (!isStoredKycName(storedName)) {
      res.status(400).json({ error: 'unsupported file' });
      return;
    }
    const clean = await scanForMalware(sellerId, storedName);
    const id = await recordKycDocument(sellerId, String(req.body.kind ?? 'government_id'), storedName, clean);
    res.status(clean ? 201 : 422).json({ id, scanStatus: clean ? 'clean' : 'infected' });
  } catch (err) {
    next(err);
  }
}

async function thumbnailHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const fileName = String(req.body?.fileName ?? '');
    const thumbnailPath = await generateThumbnail(req.user!.sellerId, fileName);
    res.sendFile(thumbnailPath);
  } catch (err) {
    next(err);
  }
}

async function downloadKycDocumentHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const fileName = String(req.query.file ?? '');
    const { body, contentType } = await readKycDocument(req.user!.sellerId, fileName);
    res.type(contentType).send(body);
  } catch (err) {
    next(err);
  }
}

async function deleteKycDocumentHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const document = await findOwnedKycDocument(req.user!.sellerId, req.params.documentId);
    if (!document) {
      res.status(404).json({ error: 'document not found' });
      return;
    }
    await deleteKycFile(req.user!.sellerId, document.storedName);
    await removeKycDocumentRecord(document.id);
    res.status(204).end();
  } catch (err) {
    next(err);
  }
}

kycRouter.use(requireAuth);
kycRouter.post('/documents', upload.single('file'), uploadKycDocumentHandler);
kycRouter.get('/documents/download', downloadKycDocumentHandler);
kycRouter.post('/thumbnails', thumbnailHandler);
kycRouter.delete('/documents/:documentId', deleteKycDocumentHandler);
