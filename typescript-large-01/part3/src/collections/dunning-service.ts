import { Notice, NoticeOutbox } from '../notifications/outbox';
import { describeError } from '../util/errors';
import { Logger } from '../util/logger';

export interface DunningCandidate {
  loanId: string;
  daysPastDue: number;
  preferredChannel: Notice['channel'];
}

export interface DunningOutcome {
  loanId: string;
  template: Notice['template'] | null;
}

function templateFor(dpd: number): Notice['template'] | null {
  if (dpd >= 90) return 'FINAL_DEMAND';
  if (dpd >= 60) return 'DUNNING_D60';
  if (dpd >= 30) return 'DUNNING_D30';
  if (dpd >= 15) return 'DUNNING_D15';
  return null;
}

/** Selects the dunning notice for each overdue loan and queues it. */
export class DunningService {
  constructor(
    private readonly notices: NoticeOutbox,
    private readonly log: Logger,
  ) {}

  async notify(candidate: DunningCandidate): Promise<DunningOutcome> {
    const template = templateFor(candidate.daysPastDue);
    if (!template) {
      return { loanId: candidate.loanId, template: null };
    }
    const row = await this.notices.enqueue({ loanId: candidate.loanId, template, channel: candidate.preferredChannel });
    try {
      await this.notices.dispatch(row);
    } catch (err) {
      this.log.debug('dunning notice delivery deferred', { loanId: candidate.loanId, error: describeError(err) });
    }
    return { loanId: candidate.loanId, template };
  }

  async run(candidates: DunningCandidate[]): Promise<DunningOutcome[]> {
    const outcomes: DunningOutcome[] = [];
    for (const c of candidates) {
      outcomes.push(await this.notify(c));
    }
    this.log.info('dunning run complete', { candidates: candidates.length });
    return outcomes;
  }
}
