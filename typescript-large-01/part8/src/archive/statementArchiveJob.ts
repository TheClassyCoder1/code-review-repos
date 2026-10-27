import path from 'path';
import { Logger } from '../util/logger';
import { compressStatementFolder } from './statementArchiver';

const LOAN_ID_PATTERN = /^LN-[0-9]{6,12}$/;
const PERIOD_PATTERN = /^[0-9]{4}-(0[1-9]|1[0-2])$/;
const SAFE_DIR_PATTERN = /^\/[A-Za-z0-9_\/.-]+$/;

export interface ArchiveRequest {
  loanId: string;
  period: string;
}

/** Monthly job that tars each loan's rendered statements into the archive area. */
export class StatementArchiveJob {
  private readonly log = new Logger('statement-archive-job');

  constructor(private readonly statementsRoot: string, private readonly archiveRoot: string) {
    for (const dir of [statementsRoot, archiveRoot]) {
      if (!SAFE_DIR_PATTERN.test(dir) || dir.includes('..')) {
        throw new Error('archive directories must be absolute paths of [A-Za-z0-9_/.-]');
      }
    }
  }

  async archive(request: ArchiveRequest): Promise<string> {
    if (!LOAN_ID_PATTERN.test(request.loanId)) {
      throw new Error('invalid loan id');
    }
    if (!PERIOD_PATTERN.test(request.period)) {
      throw new Error('invalid statement period');
    }
    const folder = path.join(this.statementsRoot, request.loanId, request.period);
    const archivePath = path.join(this.archiveRoot, `${request.loanId}_${request.period}.tar.gz`);
    await compressStatementFolder(folder, archivePath);
    this.log.info('statement folder archived loanId={} period={}', request.loanId, request.period);
    return archivePath;
  }
}
