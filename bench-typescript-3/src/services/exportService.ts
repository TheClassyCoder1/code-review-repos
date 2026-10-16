import fs from 'fs';
import path from 'path';
import { config } from '../config';
import { prisma } from '../db';

export interface ExportRow {
  fullName: string;
  email: string;
  currentStage: string;
  source: string;
  createdAt: Date;
}

function csvCell(value: string): string {
  const needsQuoting = /[",\n\r]/.test(value) || /^[=+\-@]/.test(value);
  const safe = /^[=+\-@]/.test(value) ? `'${value}` : value;
  return needsQuoting ? `"${safe.replace(/"/g, '""')}"` : safe;
}

export function buildCandidatesCsv(rows: ExportRow[]): string {
  const header = ['full_name', 'email', 'stage', 'source', 'created_at'].join(',');
  const lines = rows.map((row) =>
    [row.fullName, row.email, row.currentStage, row.source, row.createdAt.toISOString()]
      .map((cell) => csvCell(cell))
      .join(','),
  );
  return [header, ...lines].join('\n') + '\n';
}

export async function writeCandidateExport(companyId: string): Promise<string> {
  const rows = await prisma.candidate.findMany({
    where: { companyId },
    orderBy: { createdAt: 'asc' },
    select: { fullName: true, email: true, currentStage: true, source: true, createdAt: true },
  });
  const fileName = `candidates-${companyId.toLowerCase()}-${Date.now()}.csv`;
  await fs.promises.mkdir(config.exportDir, { recursive: true });
  await fs.promises.writeFile(path.join(config.exportDir, fileName), buildCandidatesCsv(rows));
  return fileName;
}

export function openExport(fileName: string): fs.ReadStream | null {
  const fullPath = path.join(config.exportDir, fileName);
  if (!fs.existsSync(fullPath)) {
    return null;
  }
  return fs.createReadStream(fullPath);
}
