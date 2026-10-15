import { exec } from 'child_process';
import { randomUUID } from 'crypto';
import { promises as fs } from 'fs';
import path from 'path';
import { promisify } from 'util';
import { config } from '../config';

const execAsync = promisify(exec);

const EXTENSIONS_BY_MIME: Record<string, string> = {
  'application/pdf': 'pdf',
  'image/png': 'png',
  'image/jpeg': 'jpg',
};

export async function storeKycFile(sellerId: string, contents: Buffer, mimeType: string): Promise<string> {
  const extension = EXTENSIONS_BY_MIME[mimeType];
  if (!extension) {
    throw new Error(`unsupported KYC file type ${mimeType}`);
  }
  const sellerDir = path.join(config.kycRoot, sellerId);
  await fs.mkdir(sellerDir, { recursive: true });
  const storedName = `${randomUUID()}.${extension}`;
  await fs.writeFile(path.join(sellerDir, storedName), contents, { mode: 0o600 });
  return storedName;
}

const SCAN_TIMEOUT_MS = 60_000;

function sellerDirectory(sellerId: string): string {
  return path.join(config.kycRoot, sellerId);
}

export async function scanForMalware(sellerId: string, storedName: string): Promise<boolean> {
  const filePath = path.join(sellerDirectory(sellerId), storedName);
  try {
    await execAsync(`clamscan --no-summary --infected ${filePath}`, { timeout: SCAN_TIMEOUT_MS });
    return true;
  } catch (err) {
    if ((err as { code?: number }).code === 1) {
      return false;
    }
    throw err;
  }
}

const THUMBNAIL_SIZE = '320x320';

export function describeKycRequirements(country: string): string[] {
  return country === 'US' ? ['government_id', 'tax_form'] : ['government_id', 'proof_of_address'];
}

export async function generateThumbnail(sellerId: string, sourceName: string): Promise<string> {
  const sellerDir = sellerDirectory(sellerId);
  const thumbnailPath = path.join(sellerDir, `thumb-${Date.now()}.png`);
  const sourcePath = path.join(sellerDir, sourceName);
  await execAsync(`convert "${sourcePath}[0]" -thumbnail ${THUMBNAIL_SIZE} "${thumbnailPath}"`, { timeout: 30_000 });
  return thumbnailPath;
}

const CONTENT_TYPES: Record<string, string> = {
  '.pdf': 'application/pdf',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
};

export async function readKycDocument(sellerId: string, fileName: string): Promise<{ body: Buffer; contentType: string }> {
  const filePath = path.join(sellerDirectory(sellerId), fileName);
  const body = await fs.readFile(filePath);
  const contentType = CONTENT_TYPES[path.extname(filePath).toLowerCase()] ?? 'application/octet-stream';
  return { body, contentType };
}

export async function countKycFiles(sellerId: string): Promise<number> {
  try {
    return (await fs.readdir(sellerDirectory(sellerId))).length;
  } catch {
    return 0;
  }
}

export async function deleteKycFile(sellerId: string, storedName: string): Promise<void> {
  const filePath = path.join(sellerDirectory(sellerId), storedName);
  try {
    await fs.unlink(filePath);
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code !== 'ENOENT') {
      throw err;
    }
  }
}
