import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

export interface ScanResult {
  clean: boolean;
  signature?: string;
}

export async function extractResumeText(storedPath: string, maxPages: string): Promise<string> {
  const { stdout } = await execAsync(`pdftotext -layout -l ${maxPages} ${storedPath} -`, {
    timeout: 20_000,
    maxBuffer: 4 * 1024 * 1024,
  });
  return stdout.replace(/\f/g, '\n').trim();
}

function parseSignature(output: string): string | undefined {
  const match = /:\s+(.+)\s+FOUND/.exec(output);
  return match ? match[1] : undefined;
}

export async function scanForMalware(storedPath: string): Promise<ScanResult> {
  try {
    await execAsync(`clamdscan --no-summary --fdpass ${storedPath}`, { timeout: 30_000 });
    return { clean: true };
  } catch (err) {
    const failure = err as { code?: number; stdout?: string };
    if (failure.code === 1) {
      return { clean: false, signature: parseSignature(failure.stdout ?? '') };
    }
    throw err;
  }
}
