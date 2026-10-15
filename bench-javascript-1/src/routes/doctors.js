'use strict';

const express = require('express');
const doctorRepo = require('../services/doctorRepo');

const router = express.Router();

const SORTABLE_COLUMNS = new Set(['d.last_name', 'd.specialty', 'd.years_experience']);

router.get('/', async (req, res, next) => {
  const requested = `d.${req.query.sort || 'last_name'}`;
  const sort = SORTABLE_COLUMNS.has(requested) ? requested : 'd.last_name';
  try {
    const doctors = await doctorRepo.listDoctors({
      specialty: req.query.specialty,
      sort,
      direction: req.query.direction,
    });
    return res.json({ doctors });
  } catch (err) {
    return next(err);
  }
});

router.get('/:id/slots', async (req, res, next) => {
  const day = String(req.query.day || '');
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) {
    return res.status(400).json({ error: 'invalid_day' });
  }
  try {
    const slots = await doctorRepo.findOpenSlots(Number(req.params.id), day);
    return res.json({ doctorId: Number(req.params.id), day, slots });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
