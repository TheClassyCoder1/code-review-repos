'use strict';

const db = require('../db');

async function listDoctors({ specialty, sort, direction }) {
  const order = direction === 'desc' ? 'DESC' : 'ASC';
  const params = [];
  let where = 'WHERE d.active = true';
  if (specialty) {
    params.push(specialty);
    where += ` AND d.specialty = $${params.length}`;
  }
  const sql = `SELECT d.id, d.first_name, d.last_name, d.specialty, d.years_experience
      FROM doctors d
      ${where}
     ORDER BY ${sort} ${order}`;
  const { rows } = await db.query(sql, params);
  return rows;
}

function overlaps(slot, booked) {
  return slot.start < booked.end_at && booked.start_at < slot.end;
}

const SLOT_GRANULARITY_MINUTES = [10, 15, 20, 30];

async function findOpenSlots(doctorId, day, slotMinutes = 20) {
  const dayStart = new Date(`${day}T00:00:00Z`);
  const dayEnd = new Date(dayStart.getTime() + 24 * 60 * 60 * 1000);
  const { rows: shifts } = await db.query(
    'SELECT start_at, end_at FROM doctor_shifts WHERE doctor_id = $1 AND start_at >= $2 AND start_at < $3',
    [doctorId, dayStart, dayEnd],
  );
  const { rows: booked } = await db.query(
    "SELECT start_at, end_at FROM appointments WHERE doctor_id = $1 AND status <> 'cancelled' AND start_at >= $2 AND start_at < $3",
    [doctorId, dayStart, dayEnd],
  );
  const slots = [];
  for (const shift of shifts) {
    for (let t = shift.start_at.getTime(); t + slotMinutes * 60000 <= shift.end_at.getTime(); t += slotMinutes * 60000) {
      const slot = { start: new Date(t), end: new Date(t + slotMinutes * 60000) };
      if (!booked.some((b) => overlaps(slot, b))) {
        slots.push(slot);
      }
    }
  }
  return slots;
}

module.exports = { listDoctors, findOpenSlots, SLOT_GRANULARITY_MINUTES };
