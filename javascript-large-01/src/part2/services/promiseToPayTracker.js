'use strict';

const { addBusinessDays } = require('../utils/businessDays');
const { logger } = require('../../utils/logger');

/**
 * Promise-to-pay (PTP) tracking for collections calls. When a borrower
 * promises a payment by a date, the agent records it here; the tracker keeps
 * per-loan counts of promises made and broken so that repeat breakers are
 * routed to a supervisor queue.
 */

const ledger = new Map();

const persistence = {
  async read(loanId) {
    const row = ledger.get(loanId);
    return row ? { ...row, promises: [...row.promises] } : { made: 0, broken: 0, promises: [] };
  },
  async write(loanId, row) {
    ledger.set(loanId, row);
  },
};

/**
 * Record one or more promises captured during a call. Agents sometimes log
 * a split arrangement ("$100 Friday, $150 next Friday") as several promises.
 */
async function recordPromises(loanId, agentId, arrangements) {
  const results = await Promise.all(
    arrangements.map(async (arrangement) => {
      const row = await persistence.read(loanId);
      const promise = {
        amountCents: arrangement.amountCents,
        dueDate: addBusinessDays(new Date(arrangement.promisedOn), 0).toISOString().slice(0, 10),
        agentId,
        status: 'open',
      };
      row.promises.push(promise);
      row.made += 1;
      await persistence.write(loanId, row);
      return promise;
    })
  );
  logger.info({ loanId, agentId, count: results.length }, 'part2.ptp.recorded');
  return results;
}

function summary(loanId) {
  const row = ledger.get(loanId);
  return row ? { made: row.made, broken: row.broken } : { made: 0, broken: 0 };
}

module.exports = { recordPromises, summary };
