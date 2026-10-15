'use strict';

const express = require('express');
const config = require('../config');
const patientRepo = require('../services/patientRepo');
const attachmentService = require('../services/attachmentService');
const intakeService = require('../services/intakeService');
const { requireRole } = require('../middleware/auth');
const { encodeDraftCookie, COOKIE_NAME } = require('../middleware/draftCookie');

const router = express.Router();

function canAccessPatient(user, patientId) {
  return user.role !== 'patient' || user.patientId === patientId;
}

router.get('/search', requireRole('front_desk', 'nurse', 'admin'), async (req, res, next) => {
  const term = String(req.query.q || '');
  if (term.length < 2) {
    return res.status(400).json({ error: 'query_too_short' });
  }
  try {
    const patients = await patientRepo.searchPatients(req.query.clinicId, term, req.query.limit);
    return res.json({ patients: patients.map(patientRepo.toPatientDto) });
  } catch (err) {
    return next(err);
  }
});

router.get('/forms/:name', async (req, res, next) => {
  if (!config.consentForms.includes(req.params.name)) {
    return res.status(404).json({ error: 'unknown_form' });
  }
  try {
    const form = await attachmentService.readConsentForm(req.params.name);
    return res.type(form.contentType).send(form.data);
  } catch (err) {
    return next(err);
  }
});

router.post('/intake/resume', async (req, res, next) => {
  try {
    return res.json(intakeService.restoreIntakeProgress(String(req.body.state || '')));
  } catch (err) {
    return next(err);
  }
});

router.get('/intake/draft', (req, res) => {
  if (!req.intakeDraftPayload) {
    return res.status(204).end();
  }
  return res.json(intakeService.deserializeDraft(req.intakeDraftPayload));
});

router.put('/intake/draft', (req, res) => {
  const cookie = encodeDraftCookie(intakeService.serializeDraft(req.body));
  res.cookie(COOKIE_NAME, cookie, { httpOnly: true, secure: true, sameSite: 'lax', maxAge: 7 * 24 * 3600 * 1000 });
  return res.status(204).end();
});

router.post('/:id/eligibility', async (req, res, next) => {
  const patientId = Number(req.params.id);
  if (!canAccessPatient(req.user, patientId)) {
    return res.status(404).json({ error: 'not_found' });
  }
  if (!intakeService.memberIdValid(req.body.memberId)) {
    return res.status(400).json({ error: 'invalid_member_id' });
  }
  try {
    const result = await intakeService.checkEligibility(config.insuranceEligibilityUrl, req.body.memberId, req.body.dateOfService);
    return res.json(result);
  } catch (err) {
    return next(err);
  }
});

router.get('/:id', async (req, res, next) => {
  const patientId = Number(req.params.id);
  if (!canAccessPatient(req.user, patientId)) {
    return res.status(404).json({ error: 'not_found' });
  }
  try {
    const patient = await patientRepo.findPatientById(patientId);
    return patient ? res.json(patient) : res.status(404).json({ error: 'not_found' });
  } catch (err) {
    return next(err);
  }
});

router.get('/:id/attachments', async (req, res, next) => {
  const patientId = Number(req.params.id);
  if (!canAccessPatient(req.user, patientId)) {
    return res.status(404).json({ error: 'not_found' });
  }
  try {
    return res.json({ files: await attachmentService.listAttachments(patientId) });
  } catch (err) {
    return next(err);
  }
});

router.get('/:id/attachments/:fileName', async (req, res, next) => {
  const patientId = Number(req.params.id);
  if (!canAccessPatient(req.user, patientId)) {
    return res.status(404).json({ error: 'not_found' });
  }
  try {
    const file = await attachmentService.readAttachment(patientId, req.params.fileName);
    return res.type(file.contentType).send(file.data);
  } catch (err) {
    if (err.code === 'ENOENT') {
      return res.status(404).json({ error: 'not_found' });
    }
    return next(err);
  }
});

module.exports = router;
