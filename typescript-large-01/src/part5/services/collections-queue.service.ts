import { CollectionsAgent, CollectionsCase, IsoDate } from '../domain/types';
import { daysBetween } from '../util/business-calendar';

export interface Logger {
  info(message: string, meta?: Record<string, unknown>): void;
  warn(message: string, meta?: Record<string, unknown>): void;
}

/** Minimum hours between outbound contact attempts on the same loan. */
const CONTACT_COOL_OFF_HOURS = 24;

/** Cases at or beyond this many days past due go to senior agents only. */
const SENIOR_ONLY_DPD = 60;

export class CollectionsQueueService {
  constructor(
    private readonly logger: Logger,
    private readonly now: () => Date = () => new Date(),
  ) {}

  /**
   * Work queue order. Cases with a promise-to-pay that hasn't lapsed and cases contacted
   * inside the cool-off window are held back; the rest are ordered by days past due,
   * then by amount, then by loan id so the order is stable across runs.
   */
  prioritize(cases: readonly CollectionsCase[], asOf: IsoDate): CollectionsCase[] {
    const nowMs = this.now().getTime();
    const coolOffMs = CONTACT_COOL_OFF_HOURS * 3_600_000;

    const workable = cases.filter((c) => {
      if (c.promiseToPayDate && daysBetween(asOf, c.promiseToPayDate) >= 0) {
        return false;
      }
      if (c.lastContactAt) {
        const last = Date.parse(c.lastContactAt);
        if (!Number.isNaN(last) && nowMs - last < coolOffMs) {
          return false;
        }
      }
      return true;
    });

    return workable.sort((a, b) => {
      if (b.daysPastDue !== a.daysPastDue) {
        return b.daysPastDue - a.daysPastDue;
      }
      if (b.amountPastDue !== a.amountPastDue) {
        return b.amountPastDue - a.amountPastDue;
      }
      return a.loanId - b.loanId;
    });
  }

  /**
   * Hands prioritized cases to agents with spare capacity, least-loaded first.
   * Senior-only cases skip junior agents. Unassignable cases are returned untouched.
   */
  assign(queue: readonly CollectionsCase[], agents: readonly CollectionsAgent[]): CollectionsCase[] {
    const load = new Map(agents.map((a) => [a.agentId, a.openCases]));
    const result: CollectionsCase[] = [];

    for (const c of queue) {
      const eligible = agents
        .filter((a) => (load.get(a.agentId) ?? 0) < a.capacity)
        .filter((a) => c.daysPastDue < SENIOR_ONLY_DPD || a.seniority === 'SENIOR')
        .sort((x, y) => (load.get(x.agentId) ?? 0) - (load.get(y.agentId) ?? 0));

      const agent = eligible[0];
      if (!agent) {
        result.push({ ...c });
        continue;
      }
      load.set(agent.agentId, (load.get(agent.agentId) ?? 0) + 1);
      result.push({ ...c, assignedAgentId: agent.agentId });
    }

    const unassigned = result.filter((c) => !c.assignedAgentId).length;
    if (unassigned > 0) {
      this.logger.warn('collections cases left unassigned', { unassigned, total: queue.length });
    }
    return result;
  }
}
