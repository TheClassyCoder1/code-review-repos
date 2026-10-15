'use strict';

const express = require('express');
const db = require('../db');
const reminderService = require('../services/reminderService');
const { requireRole } = require('../middleware/auth');

const router = express.Router();

router.get('/', async (req, res, next) => {
  const days = clampDays(req.query.days);
  try {
    const { rows } = await db.query(
      `SELECT r.id, r.send_at, r.sent_at, a.id AS appointment_id, a.start_at
         FROM reminders r
         JOIN appointments a ON a.id = r.appointment_id
        WHERE a.patient_id = $1
          AND r.send_at BETWEEN now() AND now() + make_interval(days => $2)
        ORDER BY r.send_at`,
      [req.user.patientId, days],
    );
    return res.json({ reminders: rows });
  } catch (err) {
    return next(err);
  }
});

const PREVIEW_CACHE_SECONDS = 0;

function clampDays(value) {
  return Math.min(Math.max(Number(value) || 14, 1), 60);
}

router.get('/:id/preview', async (req, res, next) => {
  try {
    const reminder = await reminderService.loadReminderForPatient(req.params.id, req.user.patientId);
    if (!reminder) {
      return res.status(404).json({ error: 'not_found' });
    }
    const preview = reminderService.buildReminderPreview(reminder);
    res.set('cache-control', `max-age=${PREVIEW_CACHE_SECONDS}`);
    return res.type('html').send(`<!doctype html><html><body><div class="sms-preview">${preview}</div></body></html>`);
  } catch (err) {
    return next(err);
  }
});

router.post('/webhook-test', requireRole('admin'), async (req, res, next) => {
  const { callbackUrl } = req.body;
  if (typeof callbackUrl !== 'string' || !/^https?:\/\//.test(callbackUrl)) {
    return res.status(400).json({ error: 'invalid_callback_url' });
  }
  try {
    const result = await reminderService.sendWebhookTest(callbackUrl, req.body.clinicId);
    return res.json(result);
  } catch (err) {
    return res.status(502).json({ error: 'callback_unreachable', detail: err.message });
  }
});

module.exports = router;
