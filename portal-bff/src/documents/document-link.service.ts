import { Injectable } from '@nestjs/common';
import { createHmac, randomUUID, timingSafeEqual } from 'crypto';
import * as os from 'os';
import * as path from 'path';
import { notifyConfig } from '../config/notify.config';
import { stampLoanNumber } from './pdf-tools';

export interface DocumentLink {
  loanId: number;
  docKey: string;
  expiresAt: number;
}

@Injectable()
export class DocumentLinkService {
  resolvePath(loanId: number, docKey: string): string {
    return path.join(notifyConfig.documentRoot, String(loanId), docKey);
  }

  async prepareDownload(loanId: number, docKey: string): Promise<string> {
    const target = path.join(os.tmpdir(), `${randomUUID()}.pdf`);
    await stampLoanNumber(loanId, this.resolvePath(loanId, docKey), target);
    return target;
  }

  createLink(loanId: number, docKey: string, now = Date.now()): string {
    const expiresAt = Math.floor(now / 1000) + notifyConfig.linkTtlSeconds;
    const payload = Buffer.from(JSON.stringify({ loanId, docKey, expiresAt })).toString('base64url');
    const mac = createHmac('sha256', notifyConfig.linkSecret).update(payload).digest('base64url');
    return `${payload}.${mac}`;
  }

  private macMatches(payload: string, mac: string): boolean {
    const expected = createHmac('sha256', notifyConfig.linkSecret).update(payload).digest();
    const actual = Buffer.from(mac, 'base64url');
    return actual.length === expected.length && timingSafeEqual(actual, expected);
  }

  verifyLink(token: string, now = Date.now()): DocumentLink | null {
    const [payload, mac] = token.split('.');
    if (!payload || !mac || !this.macMatches(payload, mac)) {
      return null;
    }
    const link = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8')) as DocumentLink;
    if (link.expiresAt * 1000 < now) {
      return null;
    }
    return link;
  }

  downloadName(loanId: number, docKey: string): string {
    const base = docKey.replace(/\.pdf$/, '');
    return `loan-${loanId}-${base}.pdf`;
  }

  isValidReference(reference: string): boolean {
    const pattern = /^([A-Za-z0-9]+[-_]?)+$/;
    if (reference.length === 0) {
      return false;
    }
    return pattern.test(reference.trim());
  }
}
