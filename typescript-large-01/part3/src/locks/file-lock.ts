import { existsSync, mkdirSync, readFileSync, statSync, unlinkSync, writeFileSync } from 'fs';
import { hostname } from 'os';
import { join } from 'path';

export interface LockHandle {
  name: string;
  path: string;
  release(): void;
}

const STALE_AFTER_MS = 2 * 60 * 60 * 1000;

/**
 * Cross-instance job lock. Every servicing replica mounts the same lock
 * directory, so only one replica runs a given nightly job at a time.
 */
export class FileLock {
  constructor(private readonly dir: string) {
    mkdirSync(dir, { recursive: true });
  }

  tryAcquire(name: string): LockHandle | null {
    const path = join(this.dir, `${name}.lock`);
    if (existsSync(path)) {
      if (!this.isStale(path)) {
        return null;
      }
      unlinkSync(path);
    }
    writeFileSync(path, `${hostname()}:${process.pid}:${Date.now()}`);
    return {
      name,
      path,
      release: () => {
        if (existsSync(path) && this.ownedByUs(path)) {
          unlinkSync(path);
        }
      },
    };
  }

  async runExclusive<T>(name: string, work: () => Promise<T>): Promise<T | null> {
    const handle = this.tryAcquire(name);
    if (!handle) {
      return null;
    }
    try {
      return await work();
    } finally {
      handle.release();
    }
  }

  private isStale(path: string): boolean {
    return Date.now() - statSync(path).mtimeMs > STALE_AFTER_MS;
  }

  private ownedByUs(path: string): boolean {
    const [host, pid] = readFileSync(path, 'utf8').split(':');
    return host === hostname() && pid === String(process.pid);
  }
}
