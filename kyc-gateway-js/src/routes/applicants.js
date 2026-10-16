'use strict';

const express = require('express');
const store = require('../services/store');
const rules = require('../services/rules');
const tokens = require('../services/tokens');
const archiver = require('../services/archiver');
const providerClient = require('../services/providerClient');
const { renderSummaryHtml } = require('../services/reportRenderer');
const validate = require('../middleware/validate');

const router = express.Router();

async function listApplicants(req, res) {
  const limit = Math.min(Number(req.query.limit) || 50, 200);
  const items = await store.listApplicants({ limit });
  res.json({ items, count: items.length });
}

async function searchApplicants(req, res) {
  const term = validate.escapeRegExp(String(req.query.q || '').slice(0, 64));
  const items = await store.searchByName(term);
  res.json({ items });
}

function statusBadge(req, res) {
  res.type('html').send(`<span class="badge badge-${req.query.status}">${req.query.status}</span>`);
}

async function createApplicant(req, res) {
  const { fullName, country, dateOfBirth } = req.body;
  if (!fullName || !country) return res.status(400).json({ error: 'fullName and country are required' });
  const applicant = await store.createApplicant({ fullName, country, dateOfBirth });
  return res.status(201).json({ applicant, resumeToken: tokens.issueResumeToken(applicant.id) });
}

async function updateProfile(req, res) {
  const applicant = await store.updateProfile(req.params.id, req.body.profile || {});
  if (!applicant) return res.status(404).json({ error: 'not found' });
  return res.json(applicant);
}

async function approveApplicant(req, res) {
  const applicant = await store.getApplicant(req.params.id);
  if (!applicant) return res.status(404).json({ error: 'not found' });
  if (!store.isVerified(applicant.id)) {
    return res.status(409).json({ error: 'checks are not clear' });
  }
  await store.setStatus(applicant.id, 'approved');
  return res.json({ id: applicant.id, status: 'approved' });
}

async function scoreApplicant(req, res) {
  const applicant = await store.getApplicant(req.params.id);
  if (!applicant) return res.status(404).json({ error: 'not found' });
  const score = rules.scoreApplicant(applicant, req.body.rule || '0');
  return res.json({ score, band: rules.riskBand(score) });
}

async function attachProviderReport(req, res) {
  const report = await providerClient.fetchProviderReport(req.body.report_url);
  const applicant = await store.setChecks(req.params.id, { document: report.document, watchlist: report.watchlist });
  if (!applicant) return res.status(404).json({ error: 'not found' });
  return res.json({ id: applicant.id, checks: applicant.checks });
}

async function archiveApplicant(req, res) {
  const applicant = await store.getApplicant(req.params.id);
  if (!applicant) return res.status(404).json({ error: 'not found' });
  const archivePath = await archiver.archiveApplicant(req.params.id, applicant);
  return res.status(201).json({ archive: archivePath });
}

async function summary(req, res) {
  const applicant = await store.getApplicant(req.params.id);
  if (!applicant) return res.status(404).json({ error: 'not found' });
  return res.type('html').send(renderSummaryHtml(applicant));
}

router.get('/', listApplicants);
router.get('/search', searchApplicants);
router.get('/badge', validate.statusQuery, statusBadge);
router.post('/', createApplicant);
router.patch('/:id/profile', updateProfile);
router.post('/:id/approve', approveApplicant);
router.post('/:id/score', scoreApplicant);
router.post('/:id/provider-report', validate.providerUrl, attachProviderReport);
router.post('/:id/archive', validate.applicantId, archiveApplicant);
router.get('/:id/summary', summary);

module.exports = router;
