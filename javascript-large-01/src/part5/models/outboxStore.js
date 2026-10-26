'use strict';

/**
 * In-memory delivery outbox standing in for the outbox table. Each job tracks
 * a single statement notification through its lifecycle:
 *
 *   pending -> in-flight -> delivered
 *                        -> pending (retry scheduled)
 *                        -> dead (permanent failure / attempts exhausted)
 */

const JOB_STATES = Object.freeze(['pending', 'in-flight', 'delivered', 'dead']);

function createOutboxStore() {
  const jobs = new Map();
  let sequence = 0;

  function nextId() {
    sequence += 1;
    return `dlv-${String(sequence).padStart(8, '0')}`;
  }

  return {
    insert(job) {
      const id = nextId();
      const row = { ...job, id };
      jobs.set(id, row);
      return { ...row };
    },

    findById(id) {
      const row = jobs.get(id);
      return row ? { ...row } : null;
    },

    findActiveForStatement(statementId) {
      for (const row of jobs.values()) {
        if (row.statementId === statementId && (row.state === 'pending' || row.state === 'in-flight')) {
          return { ...row };
        }
      }
      return null;
    },

    findByOwner(ownerId) {
      return [...jobs.values()].filter((row) => row.ownerId === ownerId).map((row) => ({ ...row }));
    },

    findDue(nowMs) {
      return [...jobs.values()]
        .filter((row) => row.state === 'pending' && row.notBefore <= nowMs)
        .sort((a, b) => a.notBefore - b.notBefore || (a.id < b.id ? -1 : 1))
        .map((row) => ({ ...row }));
    },

    /**
     * Compare-and-set update: only applies when the stored row is still in the
     * expected state. Returns the updated row, or null when another worker
     * moved it first.
     */
    updateIfState(id, expectedState, patch) {
      const row = jobs.get(id);
      if (!row || row.state !== expectedState) {
        return null;
      }
      if (patch.state && !JOB_STATES.includes(patch.state)) {
        throw new Error(`unknown job state: ${patch.state}`);
      }
      const updated = { ...row, ...patch, id: row.id };
      jobs.set(id, updated);
      return { ...updated };
    },

    size() {
      return jobs.size;
    },
  };
}

module.exports = { createOutboxStore, JOB_STATES };
