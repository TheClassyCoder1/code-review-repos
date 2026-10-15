'use strict';

const express = require('express');
const store = require('../services/store');

const router = express.Router();

async function stats(req, res) {
  const items = await store.listApplicants({ limit: Number.MAX_SAFE_INTEGER });
  const byStatus = {};
  for (const applicant of items) byStatus[applicant.status] = (byStatus[applicant.status] || 0) + 1;
  res.json({ total: items.length, byStatus });
}

async function purgeApplicant(req, res) {
  const removed = await store.deleteApplicant(req.params.id);
  if (!removed) return res.status(404).json({ error: 'not found' });
  return res.status(204).end();
}

router.get('/stats', stats);
router.delete('/applicants/:id', purgeApplicant);

module.exports = router;
