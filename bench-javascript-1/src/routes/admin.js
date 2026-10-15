'use strict';

const express = require('express');
const db = require('../db');
const userService = require('../services/userService');

const router = express.Router();

router.put('/doctors/:id/availability', async (req, res, next) => {
  const shifts = Array.isArray(req.body.shifts) ? req.body.shifts : [];
  const doctorId = Number(req.params.id);
  try {
    const inserted = await db.withTransaction(async (client) => {
      await client.query('DELETE FROM doctor_shifts WHERE doctor_id = $1 AND start_at > now()', [doctorId]);
      for (const shift of shifts) {
        await client.query(
          'INSERT INTO doctor_shifts (doctor_id, start_at, end_at) VALUES ($1, $2, $3)',
          [doctorId, new Date(shift.startAt), new Date(shift.endAt)],
        );
      }
      return shifts.length;
    });
    return res.json({ doctorId, shifts: inserted });
  } catch (err) {
    return next(err);
  }
});

router.post('/staff', async (req, res, next) => {
  const { username, email, password, role } = req.body;
  if (!userService.STAFF_ROLES.includes(role)) {
    return res.status(400).json({ error: 'invalid_role' });
  }
  try {
    const { rows } = await db.query(
      'INSERT INTO users (email, username, role, password_hash) VALUES ($1, $2, $3, $4) RETURNING id, role',
      [email, username, role, userService.hashStaffPassword(username, password)],
    );
    return res.status(201).json(rows[0]);
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
