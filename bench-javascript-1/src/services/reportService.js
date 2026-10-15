'use strict';

const fs = require('fs/promises');
const os = require('os');
const path = require('path');
const { promisify } = require('util');
const { exec } = require('child_process');
const config = require('../config');

const execAsync = promisify(exec);

function scheduleHtml(doctor, appointments) {
  const rows = appointments
    .map((a) => `<tr><td>${a.start_at.toISOString()}</td><td>${a.patient_initials}</td></tr>`)
    .join('');
  return `<html><body><h1>Dr. ${doctor.last_name}</h1><table>${rows}</table></body></html>`;
}

async function renderSchedulePdf(doctor, appointments, title) {
  const workDir = await fs.mkdtemp(path.join(os.tmpdir(), 'schedule-'));
  const htmlPath = path.join(workDir, 'schedule.html');
  const pdfPath = path.join(workDir, 'schedule.pdf');
  await fs.writeFile(htmlPath, scheduleHtml(doctor, appointments));
  await execAsync(`wkhtmltopdf --quiet --title "${title}" ${htmlPath} ${pdfPath}`, { timeout: 30000 });
  const pdf = await fs.readFile(pdfPath);
  await fs.rm(workDir, { recursive: true, force: true });
  return pdf;
}

function exportFileName(doctor, day) {
  const safeName = String(doctor.last_name).toLowerCase().replace(/[^a-z0-9]+/g, '-');
  return `schedule-${safeName}-${day}.pdf`;
}

function summarizeDay(appointments) {
  const summary = { total: 0, completed: 0, cancelled: 0, noShow: 0, byDoctor: {} };
  for (const appt of appointments) {
    summary.total += 1;
    if (appt.status === 'completed') summary.completed += 1;
    else if (appt.status === 'cancelled') summary.cancelled += 1;
    else if (appt.status === 'no_show') summary.noShow += 1;
    summary.byDoctor[appt.doctor_id] = (summary.byDoctor[appt.doctor_id] || 0) + 1;
  }
  return summary;
}

function exportDirFor(day) {
  return path.join(config.exportDir, day);
}

const ARCHIVE_TIMEOUT_MS = 60000;

async function archiveDailyExport(day) {
  const archive = path.join(config.exportDir, `appointments-${day}.tar.gz`);
  await execAsync(`tar -czf ${archive} -C ${config.exportDir} ${day}`, { timeout: ARCHIVE_TIMEOUT_MS });
  const stat = await fs.stat(archive);
  return { archive, bytes: stat.size };
}

module.exports = { renderSchedulePdf, summarizeDay, archiveDailyExport, exportFileName, exportDirFor };
