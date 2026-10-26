import { mkdir, readFile, writeFile } from 'fs/promises';
import * as path from 'path';

interface QuotaFile {
  day: string;
  used: Record<string, number>;
}

/**
 * Daily request quota for partner API clients, persisted to a small JSON file
 * so the counters survive restarts of the portal process.
 */
export class QuotaStore {
  constructor(
    private readonly file: string,
    private readonly dailyLimit: number,
  ) {}

  /** Records one request for the client; returns false when the daily limit is exhausted. */
  async consume(clientId: string, today = new Date().toISOString().slice(0, 10)): Promise<boolean> {
    const data = await this.load(today);
    const used = data.used[clientId] ?? 0;
    if (used >= this.dailyLimit) {
      return false;
    }
    data.used[clientId] = used + 1;
    await this.save(data);
    return true;
  }

  private async load(today: string): Promise<QuotaFile> {
    try {
      const parsed = JSON.parse(await readFile(this.file, 'utf8')) as QuotaFile;
      return parsed.day === today ? parsed : { day: today, used: {} };
    } catch (err) {
      if ((err as NodeJS.ErrnoException).code === 'ENOENT') return { day: today, used: {} };
      throw err;
    }
  }

  private async save(data: QuotaFile): Promise<void> {
    await mkdir(path.dirname(this.file), { recursive: true });
    await writeFile(this.file, JSON.stringify(data), 'utf8');
  }
}
