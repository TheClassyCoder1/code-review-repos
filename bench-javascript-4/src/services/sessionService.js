'use strict';

const db = require('../db');
const { httpError } = require('../errors');
const { quoteFor } = require('./pricing');

async function startSession({ lotId, plate, source }) {
  const active = await db('sessions').where({ plate }).whereNull('ended_at').first('id');
  if (active) {
    throw httpError(409, 'vehicle already has an active session');
  }
  const [session] = await db('sessions')
    .insert({ lot_id: lotId, plate, source, started_at: db.fn.now() })
    .returning('*');
  return session;
}

function elapsedMinutes(startedAt, endedAt) {
  const ms = endedAt.getTime() - new Date(startedAt).getTime();
  return Math.max(0, Math.ceil(ms / 60000));
}

const SESSION_COLUMNS = ['s.id', 's.lot_id', 'l.name as lot_name', 's.plate', 's.source', 's.started_at'];

async function endSession(sessionId, endedAt = new Date()) {
  return db.transaction(async (trx) => {
    const session = await trx('sessions').where({ id: sessionId }).whereNull('ended_at').forUpdate().first();
    if (!session) {
      return null;
    }
    const lot = await trx('lots').where({ id: session.lot_id }).first('id', 'rate_card');
    const amountCents = quoteFor(lot, elapsedMinutes(session.started_at, endedAt));
    const [closed] = await trx('sessions')
      .where({ id: sessionId })
      .update({ ended_at: endedAt, amount_cents: amountCents })
      .returning('*');
    await trx('lots').where({ id: session.lot_id }).where('occupied', '>', 0).decrement('occupied', 1);
    return closed;
  });
}

function toSessionView(row) {
  return { ...row, started_at: new Date(row.started_at).toISOString() };
}

const MAX_ACTIVE_ROWS = 500;

async function listActiveSessions({ lotId } = {}) {
  const query = db('sessions as s')
    .join('lots as l', 'l.id', 's.lot_id')
    .whereNull('s.ended_at')
    .select(SESSION_COLUMNS)
    .orderBy('s.started_at', 'asc')
    .limit(MAX_ACTIVE_ROWS);
  if (lotId) {
    query.where('s.lot_id', lotId);
  }
  const rows = await query;
  return rows.map(toSessionView);
}

module.exports = { startSession, endSession, listActiveSessions, elapsedMinutes };
