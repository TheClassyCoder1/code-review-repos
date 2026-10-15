'use strict';

const express = require('express');
const db = require('../db');
const { createBookingReference } = require('../util/tokens');
const reminderService = require('../services/reminderService');

const router = express.Router();

const APPOINTMENT_SELECT = `SELECT a.id, a.reference, a.patient_id, a.doctor_id, a.start_at, a.end_at, a.status, a.reason,
       d.first_name || ' ' || d.last_name AS doctor_name, c.name AS clinic_name, c.address AS clinic_address
  FROM appointments a
  JOIN doctors d ON d.id = a.doctor_id
  JOIN clinics c ON c.id = a.clinic_id`;

router.get('/:id', async (req, res, next) => {
  try {
    const { rows } = await db.query(`${APPOINTMENT_SELECT} WHERE a.id = $1`, [req.params.id]);
    const appt = rows[0];
    if (!appt) {
      return res.status(404).json({ error: 'not_found' });
    }
    if (req.user.role === 'patient' && appt.patient_id !== req.user.patientId) {
      return res.status(404).json({ error: 'not_found' });
    }
    return res.json(appt);
  } catch (err) {
    return next(err);
  }
});

router.get('/by-reference/:reference', async (req, res, next) => {
  try {
    const { rows } = await db.query(
      `${APPOINTMENT_SELECT} WHERE a.reference = $1 AND a.patient_id = $2`,
      [req.params.reference.toUpperCase(), req.user.patientId],
    );
    return rows[0] ? res.json(rows[0]) : res.status(404).json({ error: 'not_found' });
  } catch (err) {
    return next(err);
  }
});

router.get('/:id/confirmation', async (req, res, next) => {
  try {
    const { rows } = await db.query(`${APPOINTMENT_SELECT} WHERE a.id = $1`, [req.params.id]);
    const appt = rows[0];
    if (!appt || (req.user.role === 'patient' && appt.patient_id !== req.user.patientId)) {
      return res.status(404).send('Appointment not found');
    }
    const when = new Date(appt.start_at).toUTCString();
    return res.type('html').send(`<!doctype html>
<html><head><title>Appointment ${appt.reference}</title></head>
<body>
  <h1>Your appointment is confirmed</h1>
  <p>${appt.doctor_name} at ${appt.clinic_name}, ${appt.clinic_address}</p>
  <p>When: ${when}</p>
  <p>Reason for visit: ${appt.reason}</p>
</body></html>`);
  } catch (err) {
    return next(err);
  }
});

function minutesBetween(start, end) {
  return Math.round((end.getTime() - start.getTime()) / 60000);
}

const DEFAULT_VISIT_MINUTES = 20;

router.post('/', async (req, res, next) => {
  const { doctorId, startAt, reason } = req.body;
  const patientId = req.user.role === 'patient' ? req.user.patientId : Number(req.body.patientId);
  const start = new Date(startAt);
  if (!Number.isInteger(Number(doctorId)) || !patientId || Number.isNaN(start.getTime())) {
    return res.status(400).json({ error: 'invalid_request' });
  }
  const end = new Date(start.getTime() + DEFAULT_VISIT_MINUTES * 60 * 1000);
  try {
    const booked = await db.withTransaction(async (client) => {
      const { rows } = await client.query(
        `INSERT INTO appointments (reference, patient_id, doctor_id, clinic_id, start_at, end_at, status, reason)
         SELECT $1, $2, d.id, d.clinic_id, $4, $5, 'booked', $6 FROM doctors d WHERE d.id = $3
         RETURNING id, reference, start_at, end_at`,
        [createBookingReference(start), patientId, Number(doctorId), start, end, String(reason || '').slice(0, 500)],
      );
      if (rows.length === 0) {
        return null;
      }
      for (const sendAt of reminderService.computeReminderTimes(start)) {
        await client.query('INSERT INTO reminders (appointment_id, send_at) VALUES ($1, $2)', [rows[0].id, sendAt]);
      }
      return rows[0];
    });
    if (!booked) {
      return res.status(404).json({ error: 'doctor_not_found' });
    }
    return res.status(201).json({ ...booked, durationMinutes: minutesBetween(start, end) });
  } catch (err) {
    if (err.code === '23P01') {
      return res.status(409).json({ error: 'slot_taken' });
    }
    return next(err);
  }
});

router.get('/', async (req, res, next) => {
  try {
    const { rows } = await db.query(`${APPOINTMENT_SELECT} WHERE a.patient_id = $1 ORDER BY a.start_at DESC LIMIT 50`, [req.user.patientId]);
    return res.json({ appointments: rows });
  } catch (err) {
    return next(err);
  }
});

router.delete('/:id', async (req, res, next) => {
  try {
    const { rows } = await db.query(
      `UPDATE appointments SET status = 'cancelled', cancelled_at = now()
        WHERE id = $1 AND status = 'booked'
        RETURNING id, status, cancelled_at`,
      [req.params.id],
    );
    if (rows.length === 0) {
      return res.status(404).json({ error: 'not_found' });
    }
    await db.query('DELETE FROM reminders WHERE appointment_id = $1 AND sent_at IS NULL', [rows[0].id]);
    return res.json(rows[0]);
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
