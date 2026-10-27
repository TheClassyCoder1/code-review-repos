'use strict';

/**
 * In-memory repository for payment plans, standing in for the primary
 * datastore table. Plans carry the loan id and the owning user id; only one
 * plan per loan may be in the "active" state at a time.
 */

const plans = new Map();
let sequence = 0;

function nextId() {
  sequence += 1;
  return `plan-${String(sequence).padStart(6, '0')}`;
}

function insert(plan) {
  const row = { ...plan, id: nextId(), createdAt: new Date().toISOString() };
  plans.set(row.id, row);
  return row;
}

function findById(id) {
  return plans.get(id) || null;
}

function findActiveByLoan(loanId) {
  for (const plan of plans.values()) {
    if (plan.loanId === loanId && plan.status === 'active') {
      return plan;
    }
  }
  return null;
}

function updateStatus(id, status) {
  const plan = plans.get(id);
  if (!plan) {
    return null;
  }
  plan.status = status;
  plan.updatedAt = new Date().toISOString();
  return plan;
}

/**
 * Page through plans in a given status, newest first. The cursor is the id of
 * the last row the caller saw; ids are monotonically increasing so they double
 * as a stable sort key. Returns the page and the cursor for the next page, or
 * null when there are no more rows.
 */
function listByStatus(status, { limit = 50, after = null } = {}) {
  const pageSize = Math.min(Math.max(Number(limit) || 50, 1), 200);

  const matching = [...plans.values()]
    .filter((p) => p.status === status)
    .sort((a, b) => (a.id < b.id ? 1 : a.id > b.id ? -1 : 0));

  let start = 0;
  if (after) {
    const idx = matching.findIndex((p) => p.id === after);
    start = idx === -1 ? matching.length : idx + 1;
  }

  const page = matching.slice(start, start + pageSize);
  const hasMore = start + pageSize < matching.length;

  return {
    plans: page,
    nextCursor: hasMore && page.length > 0 ? page[page.length - 1].id : null,
  };
}

function _reset() {
  plans.clear();
  sequence = 0;
}

module.exports = { insert, findById, findActiveByLoan, updateStatus, listByStatus, _reset };
