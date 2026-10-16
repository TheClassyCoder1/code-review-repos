'use strict';

const { parentPort, workerData } = require('node:worker_threads');
const knex = require('knex');

function summarize(samples) {
  const byLot = new Map();
  for (const sample of samples) {
    const entry = byLot.get(sample.lot_id) || { lotId: sample.lot_id, peak: 0, total: 0, count: 0 };
    entry.peak = Math.max(entry.peak, sample.occupied);
    entry.total += sample.occupied;
    entry.count += 1;
    byLot.set(sample.lot_id, entry);
  }
  return byLot;
}

async function main() {
  const db = knex({ client: 'pg', connection: workerData.databaseUrl, pool: { min: 0, max: 2 } });
  try {
    const dayStart = new Date(`${workerData.reportDate}T00:00:00Z`);
    const dayEnd = new Date(dayStart.getTime() + 24 * 60 * 60 * 1000);
    const samples = await db('occupancy_events')
      .where('recorded_at', '>=', dayStart)
      .andWhere('recorded_at', '<', dayEnd)
      .select('lot_id', 'occupied');
    const lots = await db('lots').select('id', 'name', 'capacity');
    const stats = summarize(samples);
    const rows = lots.map((lot) => {
      const s = stats.get(lot.id) || { peak: 0, total: 0, count: 0 };
      return { lotId: lot.id, name: lot.name, capacity: lot.capacity, peak: s.peak, avg: s.count ? s.total / s.count : 0 };
    });
    parentPort.postMessage(rows);
  } finally {
    await db.destroy();
  }
}

main();
