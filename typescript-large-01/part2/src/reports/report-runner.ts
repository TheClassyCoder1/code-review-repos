import { spawn } from 'child_process';
import { log } from '../support/log';
import { ReportRequest } from './report-type.guard';

const RENDER_TIMEOUT_MS = 60_000;

/**
 * Runs the servicing team's report renderer (a shell wrapper around the
 * legacy LaTeX templates) and collects the PDF it writes to stdout.
 */
export class ReportRunner {
  constructor(private readonly reportsDir: string) {}

  render(request: ReportRequest): Promise<Buffer> {
    return new Promise((resolve, reject) => {
      const child = spawn(`./bin/render-report --type ${request.type} --loan ${request.loanId} --year ${request.year}`, {
        cwd: this.reportsDir,
        shell: true,
        stdio: ['ignore', 'pipe', 'pipe'],
        timeout: RENDER_TIMEOUT_MS,
      });
      const chunks: Buffer[] = [];
      child.stdout.on('data', (chunk: Buffer) => chunks.push(chunk));
      child.stderr.on('data', (chunk: Buffer) => {
        log.debug('report renderer stderr', { loanId: request.loanId, output: chunk.toString('utf8').slice(0, 200) });
      });
      child.on('error', reject);
      child.on('close', (code) => {
        if (code !== 0) {
          reject(new Error(`report renderer exited with ${code ?? 'signal'}`));
          return;
        }
        resolve(Buffer.concat(chunks));
      });
    });
  }
}
