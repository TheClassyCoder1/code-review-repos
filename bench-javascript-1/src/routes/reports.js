'use strict';

const express = require('express');
const db = require('../db');
const reportService = require('../services/reportService');

const router = express.Router();

const DAY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

router.get('/doctors/:id/schedule.pdf', async (req, res, next) => {
  try {
    const { rows: doctors } = await db.query('SELECT id, last_name FROM doctors WHERE id = $1', [req.params.id]);
    if (doctors.length === 0) {
      return res.status(404).json({ error: 'not_found' });
    }
    const { rows: appointments } = await db.query(
      "SELECT a.start_at, upper(left(p.first_name, 1) || left(p.last_name, 1)) AS patient_initials FROM appointments a JOIN patients p ON p.id = a.patient_id WHERE a.doctor_id = $1 AND a.start_at::date = current_date AND a.status = 'booked' ORDER BY a.start_at",
      [req.params.id],
    );
    const title = req.query.title || `Schedule for Dr. ${doctors[0].last_name}`;
    const pdf = await reportService.renderSchedulePdf(doctors[0], appointments, title);
    return res.type('application/pdf').send(pdf);
  } catch (err) {
    return next(err);
  }
});

router.get('/daily/:day', async (req, res, next) => {
  if (!DAY_PATTERN.test(req.params.day)) {
    return res.status(400).json({ error: 'invalid_day' });
  }
  try {
    const { rows } = await db.query('SELECT doctor_id, status FROM appointments WHERE start_at::date = $1', [req.params.day]);
    return res.json(reportService.summarizeDay(rows));
  } catch (err) {
    return next(err);
  }
});

router.post('/exports/:day/archive', async (req, res, next) => {
  const { day } = req.params;
  if (!DAY_PATTERN.test(day)) {
    return res.status(400).json({ error: 'invalid_day' });
  }
  try {
    return res.status(201).json(await reportService.archiveDailyExport(day));
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
