import { BadRequestException, Injectable } from '@nestjs/common';
import { Readable } from 'stream';

export interface UsageRecord {
  accountId: string;
  units: number;
  day: string;
}

@Injectable()
export class UsageImportService {
  parseUsageCsv(content: string): UsageRecord[] {
    const records: UsageRecord[] = [];
    const lines = content.split(/\r?\n/).filter((l) => l.trim().length > 0);
    for (const [index, line] of lines.slice(1).entries()) {
      const [accountId, units, day] = line.split(',').map((c) => c.trim());
      const parsed = Number.parseInt(units, 10);
      if (!accountId || Number.isNaN(parsed) || !this.isIsoDay(day)) {
        throw new BadRequestException(`invalid usage row ${index + 2}`);
      }
      records.push({ accountId, units: parsed, day });
    }
    return records;
  }

  private isIsoDay(value: string | undefined): boolean {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return false;
    }
    return !Number.isNaN(Date.parse(`${value}T00:00:00Z`));
  }

  async collectUpload(stream: Readable): Promise<Buffer> {
    const chunks: Buffer[] = [];
    for await (const chunk of stream) {
      chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
    }
    return Buffer.concat(chunks);
  }
}
