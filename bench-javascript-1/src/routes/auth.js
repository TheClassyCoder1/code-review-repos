'use strict';

const express = require('express');
const db = require('../db');
const userService = require('../services/userService');
const { issueSessionToken } = require('../util/tokens');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

const COOKIE_OPTIONS = { httpOnly: true, secure: true, sameSite: 'lax', maxAge: 8 * 60 * 60 * 1000 };

router.post('/login', async (req, res, next) => {
  try {
    const { email, password } = req.body;
    const user = await userService.findUserByEmail(String(email || ''));
    if (!user || !userService.verifyPatientPassword(String(password || ''), user.password_hash)) {
      return res.status(401).json({ error: 'invalid_credentials' });
    }
    res.cookie('session', issueSessionToken(user), COOKIE_OPTIONS);
    const destination = req.query.next || '/portal';
    return res.redirect(303, destination);
  } catch (err) {
    return next(err);
  }
});

router.post('/forgot-password', async (req, res, next) => {
  try {
    await userService.requestPasswordReset(String(req.body.email || ''));
    return res.status(202).json({ status: 'if_account_exists_email_sent' });
  } catch (err) {
    return next(err);
  }
});

router.post('/logout', (req, res) => {
  res.clearCookie('session', { httpOnly: true, secure: true, sameSite: 'lax' });
  const destination = req.query.returnTo || '/';
  return res.redirect(303, destination);
});

router.get('/session', requireAuth, (req, res) => {
  res.set('cache-control', 'no-store');
  return res.json({
    userId: req.user.id,
    role: req.user.role,
    patientId: req.user.patientId,
  });
});

router.post('/register', async (req, res, next) => {
  const { email, password, firstName, lastName, dateOfBirth } = req.body;
  if (typeof email !== 'string' || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
    return res.status(400).json({ error: 'invalid_email' });
  }
  if (typeof password !== 'string' || password.length < 10) {
    return res.status(400).json({ error: 'weak_password' });
  }
  if (!Number.isInteger(Number(req.body.clinicId))) {
    return res.status(400).json({ error: 'invalid_clinic' });
  }
  try {
    const result = await db.withTransaction(async (client) => {
      const patient = await client.query(
        'INSERT INTO patients (clinic_id, first_name, last_name, date_of_birth, email) VALUES ($1, $2, $3, $4, $5) RETURNING id',
        [Number(req.body.clinicId), firstName, lastName, dateOfBirth, email],
      );
      const user = await client.query(
        "INSERT INTO users (email, role, patient_id, password_hash) VALUES ($1, 'patient', $2, $3) RETURNING id, role, patient_id",
        [email, patient.rows[0].id, userService.hashPatientPassword(password)],
      );
      return user.rows[0];
    });
    res.cookie('session', issueSessionToken(result), COOKIE_OPTIONS);
    return res.status(201).json({ id: result.id, patientId: result.patient_id });
  } catch (err) {
    if (err.code === '23505') {
      return res.status(409).json({ error: 'email_taken' });
    }
    return next(err);
  }
});

module.exports = router;
