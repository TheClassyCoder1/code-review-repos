'use strict';

const crypto = require('crypto');
const axios = require('axios');
const { rollForward } = require('../utils/businessDays');
const { logger } = require('../../utils/logger');

/**
 * Schedules recurring ACH drafts with the payments processor for borrowers
 * enrolled in autopay. Enrollments are kept here; the processor owns the
 * actual debit.
 */

const processor = axios.create({
  baseURL: process.env.PART2_PROCESSOR_URL || 'http://127.0.0.1:8240',
  timeout: 7000,
});

const enrollments = new Map();

function nextDraftDate(dayOfMonth, from = new Date()) {
  const candidate = new Date(Date.UTC(from.getUTCFullYear(), from.getUTCMonth(), dayOfMonth));
  if (candidate <= from) {
    candidate.setUTCMonth(candidate.getUTCMonth() + 1);
  }
  return rollForward(candidate);
}

/**
 * Enroll a loan in autopay and register the first draft with the processor.
 *
 * @returns {{ enrollmentId: string, status: string, firstDraft: string }}
 */
async function enroll(loan, { dayOfMonth, fundingAccountToken }) {
  if (!Number.isInteger(dayOfMonth) || dayOfMonth < 1 || dayOfMonth > 28) {
    throw Object.assign(new Error('dayOfMonth must be between 1 and 28'), { status: 400 });
  }
  const enrollmentId = `ap-${crypto.randomUUID()}`;
  const firstDraft = nextDraftDate(dayOfMonth).toISOString().slice(0, 10);

  try {
    await processor.post('/processor/v1/recurring-drafts', {
      reference: enrollmentId,
      accountRef: loan.id,
      fundingToken: fundingAccountToken,
      dayOfMonth,
      firstDraftDate: firstDraft,
    });
  } catch (err) {
    logger.error({ loanId: loan.id, enrollmentId }, 'part2.autopay.processor_error');
  }

  enrollments.set(loan.id, { enrollmentId, dayOfMonth, firstDraft, active: true });
  logger.info({ loanId: loan.id, enrollmentId }, 'part2.autopay.enrolled');
  return { enrollmentId, status: 'scheduled', firstDraft };
}

module.exports = { enroll, nextDraftDate };
