'use strict';

/**
 * In-memory statement store standing in for the primary datastore repository.
 * Statements are keyed by id and carry the owning user id so ownership can be
 * enforced at the service layer.
 */

const statements = new Map();

function seed() {
  const rows = [
    { id: 'stmt-1001', ownerId: 'user-7', period: '2026-09', total: 1240.55, fileName: 'stmt-1001.pdf' },
    { id: 'stmt-1002', ownerId: 'user-7', period: '2026-10', total: 1190.0, fileName: 'stmt-1002.pdf' },
    { id: 'stmt-2001', ownerId: 'user-42', period: '2026-09', total: 980.25, fileName: 'stmt-2001.pdf' },
    { id: 'stmt-2002', ownerId: 'user-42', period: '2026-10', total: 1012.5, fileName: 'stmt-2002.pdf' },
  ];
  for (const row of rows) {
    statements.set(row.id, row);
  }
}

seed();

function findById(id) {
  return statements.get(id) || null;
}

function findByOwner(ownerId) {
  return [...statements.values()].filter((s) => s.ownerId === ownerId);
}

function save(statement) {
  statements.set(statement.id, statement);
  return statement;
}

module.exports = { findById, findByOwner, save, _store: statements };
