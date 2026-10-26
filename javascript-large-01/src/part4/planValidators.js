'use strict';

const { ValidationError, assertSafeId } = require('../utils/validate');
const { parseIsoDay, daysBetween, toIsoDay } = require('./businessDays');
const { collectionsConfig } = require('./config');

/**
 * Request validation for the payment-plan endpoints. Validators return a new,
 * normalised object and never pass caller-supplied keys through, so handlers
 * only ever see the fields listed here.
 */

const MAX_FIRST_DUE_OFFSET_DAYS = 45;

/**
 * Validate and normalise a "create plan" request body. The first due date must
 * be a real calendar day no earlier than tomorrow and no later than the
 * servicing agreement's maximum offset.
 */
function validatePlanRequest(body, today = toIsoDay(new Date())) {
  if (!body || typeof body !== 'object') {
    throw new ValidationError('request body must be an object');
  }

  const loanId = assertSafeId(body.loanId);

  const balance = Number(body.balance);
  if (!Number.isFinite(balance) || balance <= 0 || balance > 1000000) {
    throw new ValidationError('balance must be a positive amount up to 1,000,000');
  }

  const installments = Number(body.installments);
  const { minInstallments, maxInstallments } = collectionsConfig.plans;
  if (!Number.isInteger(installments) || installments < minInstallments || installments > maxInstallments) {
    throw new ValidationError(`installments must be an integer between ${minInstallments} and ${maxInstallments}`);
  }

  let firstDueDay;
  try {
    firstDueDay = toIsoDay(parseIsoDay(String(body.firstDueDay)));
  } catch (err) {
    throw new ValidationError('firstDueDay must be a calendar day in YYYY-MM-DD form');
  }
  const offset = daysBetween(today, firstDueDay);
  if (offset < 1 || offset > MAX_FIRST_DUE_OFFSET_DAYS) {
    throw new ValidationError(`firstDueDay must be between 1 and ${MAX_FIRST_DUE_OFFSET_DAYS} days from today`);
  }

  return { loanId, balance: Math.round(balance * 100) / 100, installments, firstDueDay };
}

/**
 * Parse `limit` and `after` paging parameters from a query string object.
 * Limit defaults to 25 and is clamped to [1, 100]; the cursor must look like a
 * plan id when present.
 */
function parsePageQuery(query) {
  const source = query || {};
  let limit = 25;

  if (source.limit !== undefined) {
    const parsed = Number.parseInt(String(source.limit), 10);
    if (!Number.isFinite(parsed)) {
      throw new ValidationError('limit must be an integer');
    }
    limit = Math.min(Math.max(parsed, 1), 100);
  }

  let after = null;
  if (source.after !== undefined && source.after !== '') {
    const cursor = String(source.after);
    if (!/^plan-\d{6,}$/.test(cursor)) {
      throw new ValidationError('after must be a plan id');
    }
    after = cursor;
  }

  return { limit, after };
}

module.exports = { validatePlanRequest, parsePageQuery, MAX_FIRST_DUE_OFFSET_DAYS };
