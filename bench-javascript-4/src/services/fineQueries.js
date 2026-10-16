'use strict';

const db = require('../db');
const { fineBalance } = require('./fineService');

const FINE_COLUMNS = ['id', 'lot_id', 'plate', 'reason', 'amount_cents', 'late_fee_cents', 'status', 'issued_at', 'due_at'];
const MAX_PAGE_SIZE = 200;

async function getFineDetail(fineId) {
  const fine = await db('fines').where({ id: fineId }).first(FINE_COLUMNS);
  if (!fine) {
    return null;
  }
  const events = await db('fine_events').where({ fine_id: fineId }).orderBy('created_at', 'asc').select('type', 'created_at');
  const balance = await fineBalance(fineId);
  return { ...fine, due_cents: balance.dueCents, events };
}

function csvCell(value) {
  const text = value instanceof Date ? value.toISOString() : String(value ?? '');
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

async function listFines({ plate, status, page, limit }) {
  const pageSize = Math.min(Number(limit) || 50, MAX_PAGE_SIZE);
  const pageNumber = Number(page) || 1;
  const query = db('fines')
    .select(FINE_COLUMNS)
    .orderBy('issued_at', 'desc')
    .limit(pageSize)
    .offset(pageNumber * pageSize);
  if (plate) query.where({ plate });
  if (status) query.where({ status });
  const items = await query;
  return { page: pageNumber, limit: pageSize, items };
}

const CSV_HEADER = FINE_COLUMNS.join(',');

function toCsvLine(row) {
  return FINE_COLUMNS.map((col) => csvCell(row[col])).join(',');
}

async function exportFinesCsv({ status } = {}) {
  const query = db('fines').select(FINE_COLUMNS).orderBy('id', 'asc');
  if (status) {
    query.where({ status });
  }
  const rows = await query;
  const lines = rows.map(toCsvLine);
  return [CSV_HEADER, ...lines].join('\n');
}

module.exports = { getFineDetail, listFines, exportFinesCsv };
