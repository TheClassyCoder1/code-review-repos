'use strict';

const express = require('express');
const payouts = require('../services/payoutService');
const { requireRole } = require('../middleware/auth');
const { httpError, isPositiveInt, parsePage } = require('../lib/validate');

const WEEK_START = /^\d{4}-\d{2}-\d{2}$/;

async function getPayout(req, res) {
  const payoutId = Number(req.params.id);
  if (!isPositiveInt(payoutId)) throw httpError(400, 'invalid payout id');
  const payout = await payouts.findPayoutForUser(payoutId, req.user.id);
  if (!payout) throw httpError(404, 'payout not found');
  res.json(payout);
}

async function listMyPayouts(req, res) {
  const courierId = await payouts.courierIdForUser(req.user.id);
  if (!courierId) throw httpError(404, 'courier profile not found');
  res.json({ payouts: await payouts.listCourierPayouts(courierId, parsePage(req.query)) });
}

async function getPayoutLines(req, res) {
  const payoutId = Number(req.params.id);
  if (!isPositiveInt(payoutId)) throw httpError(400, 'invalid payout id');
  const payout = await payouts.findPayoutForUser(payoutId, req.user.id);
  if (!payout) throw httpError(404, 'payout not found');
  res.json({ payoutId, lines: await payouts.listPayoutLines(payoutId) });
}

async function runWeeklyPayout(req, res) {
  const { courierId, weekStart } = req.body;
  if (!isPositiveInt(courierId) || typeof weekStart !== 'string' || !WEEK_START.test(weekStart)) {
    throw httpError(400, 'courierId and weekStart (YYYY-MM-DD) are required');
  }
  const payout = await payouts.createWeeklyPayout(courierId, weekStart);
  if (!payout) return res.status(204).end();
  return res.status(201).json(payout);
}

async function releasePayout(req, res) {
  const payoutId = Number(req.params.id);
  if (!isPositiveInt(payoutId)) throw httpError(400, 'invalid payout id');
  res.json(await payouts.releasePayout(payoutId));
}

const router = express.Router();
router.get('/', listMyPayouts);
router.get('/:id', getPayout);
router.get('/:id/lines', getPayoutLines);
router.post('/weekly', requireRole('ops'), runWeeklyPayout);
router.post('/:id/release', requireRole('ops'), releasePayout);

module.exports = router;
