'use strict';

const express = require('express');
const store = require('../services/store');
const tokens = require('../services/tokens');

const router = express.Router();

async function resumeSession(req, res) {
  const applicantId = tokens.readResumeToken(req.body.resume_token);
  if (!applicantId) return res.status(401).json({ error: 'invalid resume token' });
  const applicant = await store.getApplicant(applicantId);
  if (!applicant) return res.status(404).json({ error: 'not found' });
  const upload = tokens.issueUploadToken(applicant.id);
  return res.json({ applicant: { id: applicant.id, status: applicant.status, profile: applicant.profile }, upload });
}

router.post('/resume', resumeSession);

module.exports = router;
