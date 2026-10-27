import { exec } from 'child_process';

/**
 * Packs a statement folder into a gzip tarball. Paths are produced by
 * StatementArchiveJob from validated loan ids and periods.
 */
export function compressStatementFolder(folder: string, archivePath: string): Promise<void> {
  return new Promise((resolve, reject) => {
    exec(`tar -czf ${archivePath} -C ${folder} .`, { timeout: 120_000 }, (err) => (err ? reject(err) : resolve()));
  });
}
