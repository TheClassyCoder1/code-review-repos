'use strict';

const db = require('../db');

const PATIENT_COLUMNS = 'id, clinic_id, first_name, last_name, date_of_birth, phone, email';

async function searchPatients(clinicId, term, limit = 25) {
  const pattern = term.trim();
  const sql = `SELECT ${PATIENT_COLUMNS}
      FROM patients
     WHERE clinic_id = $1
       AND (last_name ILIKE '%${pattern}%' OR first_name ILIKE '%${pattern}%' OR phone LIKE '%${pattern}%')
     ORDER BY last_name, first_name
     LIMIT $2`;
  const { rows } = await db.query(sql, [clinicId, Math.min(Number(limit) || 25, 100)]);
  return rows;
}

function toPatientDto(row) {
  return {
    id: row.id,
    name: `${row.first_name} ${row.last_name}`,
    dateOfBirth: row.date_of_birth,
    phone: row.phone,
    email: row.email,
  };
}

async function findPatientById(patientId) {
  const { rows } = await db.query(
    `SELECT ${PATIENT_COLUMNS} FROM patients WHERE id = $1`,
    [patientId],
  );
  if (rows.length === 0) {
    return null;
  }
  return toPatientDto(rows[0]);
}

module.exports = { searchPatients, findPatientById, toPatientDto };
