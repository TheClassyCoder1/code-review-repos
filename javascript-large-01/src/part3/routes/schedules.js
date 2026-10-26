'use strict';

const express = require('express');
const { authenticate } = require('../../middleware/auth');
const { getRateCard } = require('../services/rateCardCache');
const { buildSchedule, minimumViablePayment } = require('../services/scheduleBuilder');
const { part3Config } = require('../config/part3Config');
const { ValidationError } = require('../../utils/validate');

const router = express.Router();

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function assertKnownProduct(productCode) {
  if (typeof productCode !== 'string' || !part3Config.products.includes(productCode)) {
    throw new ValidationError('unknown product code');
  }
  return productCode;
}

/**
 * Validate calculator input so the schedule always terminates within the
 * product's maximum term: the payment must cover the first month's interest
 * plus enough principal to clear the balance in maxTermMonths.
 */
function validateScheduleInput(body, card) {
  const principal = Number(body.principal);
  const payment = Number(body.payment);
  const { maxPrincipal, maxTermMonths } = part3Config.schedules;

  if (!Number.isFinite(principal) || principal <= 0 || principal > maxPrincipal) {
    throw new ValidationError(`principal must be between 0 and ${maxPrincipal}`);
  }
  if (!Number.isFinite(payment) || payment < card.minPayment) {
    throw new ValidationError(`payment must be at least ${card.minPayment}`);
  }
  const floor = Math.max(minimumViablePayment(principal, card.monthlyRate), principal / maxTermMonths + principal * card.monthlyRate);
  if (payment < floor) {
    throw new ValidationError(`payment must be at least ${Math.ceil(floor)} to clear the loan`);
  }
  const firstDueDate = typeof body.firstDueDate === 'string' && ISO_DATE.test(body.firstDueDate)
    ? body.firstDueDate
    : new Date().toISOString().slice(0, 10);
  return { principal, payment, firstDueDate };
}

/**
 * Amortisation calculator used by the portal's "pay more each month" widget.
 */
router.post('/', authenticate(), (req, res, next) => {
  try {
    const productCode = assertKnownProduct(req.body.productCode);
    const card = getRateCard(productCode);
    const input = validateScheduleInput(req.body, card);
    const schedule = buildSchedule({ ...input, monthlyRate: card.monthlyRate });
    return res.json({ productCode, annualRate: card.annualRate, ...schedule });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
