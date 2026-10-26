'use strict';

/**
 * In-memory dispute repository, mirroring the statement store. Every dispute
 * carries the owning user id and the statement it was raised against, plus an
 * append-only history of status changes.
 */

const disputes = new Map();
let sequence = 5000;

function nextId() {
  sequence += 1;
  return `dsp-${sequence}`;
}

function clone(dispute) {
  return dispute ? { ...dispute, history: dispute.history.map((h) => ({ ...h })) } : null;
}

function insert(dispute) {
  const id = dispute.id || nextId();
  const row = { ...dispute, id, history: dispute.history ? [...dispute.history] : [] };
  disputes.set(id, row);
  return clone(row);
}

function findById(id) {
  return clone(disputes.get(id) || null);
}

function findByOwner(ownerId) {
  return [...disputes.values()].filter((d) => d.ownerId === ownerId).map(clone);
}

function findByStatement(statementId) {
  return [...disputes.values()].filter((d) => d.statementId === statementId).map(clone);
}

function findAll(predicate = () => true) {
  return [...disputes.values()].filter(predicate).map(clone);
}

/**
 * Apply a partial update and append a history entry in one step so the two
 * cannot drift apart.
 */
function update(id, changes, historyEntry) {
  const current = disputes.get(id);
  if (!current) {
    return null;
  }
  const next = { ...current, ...changes, updatedAt: Date.now() };
  next.history = historyEntry ? [...current.history, historyEntry] : current.history;
  disputes.set(id, next);
  return clone(next);
}

function clear() {
  disputes.clear();
  sequence = 5000;
}

module.exports = {
  insert,
  findById,
  findByOwner,
  findByStatement,
  findAll,
  update,
  clear,
};
