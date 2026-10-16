'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { Worker } = require('node:worker_threads');
const { config } = require('../config');

const WORKER_PATH = path.join(__dirname, '..', 'workers', 'occupancyWorker.js');

function runOccupancyReport(reportDate) {
  return new Promise((resolve, reject) => {
    const worker = new Worker(WORKER_PATH, { workerData: { reportDate, databaseUrl: config.databaseUrl } });
    worker.once('message', resolve);
    worker.once('error', reject);
    worker.once('exit', (code) => {
      if (code !== 0) reject(new Error(`occupancy worker exited with code ${code}`));
    });
  });
}

function csvEscape(value) {
  const text = String(value);
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

const REPORT_HEADER = 'lot_id,lot_name,capacity,peak_occupied,avg_occupied\n';

async function writeReport(rows, reportDate) {
  const filePath = path.join(config.exportDir, `occupancy-${reportDate}.csv`);
  const handle = await fs.promises.open(filePath, 'w');
  if (rows.length === 0) {
    return null;
  }
  await handle.write(REPORT_HEADER);
  for (const row of rows) {
    await handle.write(`${row.lotId},${csvEscape(row.name)},${row.capacity},${row.peak},${row.avg.toFixed(1)}\n`);
  }
  await handle.close();
  return filePath;
}

async function generateDailyReport(reportDate) {
  const rows = await runOccupancyReport(reportDate);
  return writeReport(rows, reportDate);
}

if (require.main === module) {
  const reportDate = process.argv[2] || new Date(Date.now() - 86400000).toISOString().slice(0, 10);
  generateDailyReport(reportDate)
    .then((file) => console.log(file ? `report written to ${file}` : 'no occupancy data'))
    .catch((err) => {
      console.error(err);
      process.exitCode = 1;
    });
}

module.exports = { runOccupancyReport, writeReport, generateDailyReport };
