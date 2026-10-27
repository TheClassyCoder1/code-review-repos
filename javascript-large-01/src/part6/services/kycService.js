'use strict';

const kycRecordStore = require('../models/kycRecordStore');
const insightsClient = require('./insightsClient');
const { formatStatementPeriod } = require('../utils/statementPeriodFormat');
const { logger } = require('../../utils/logger');

/**
 * Identity verification for statement customers. The portal collects the
 * customer's legal name, date of birth and national identifier; once the
 * identifier passes the format check the record is stored and the customer is
 * marked verified from the current statement period onwards.
 */

const NATIONAL_ID_FORMATS = {
  GB: /^[A-CEGHJ-PR-TW-Z]{2}\d{6}[A-D]$/,
  US: /^\d{3}-?\d{2}-?\d{4}$/,
  IN: /^\d{12}$/,
  DE: /^[0-9A-Z]{9,10}$/,
};

class KycError extends Error {
  constructor(message, status = 422) {
    super(message);
    this.name = 'KycError';
    this.status = status;
  }
}

function normaliseNationalId(value) {
  return String(value || '').replace(/\s+/g, '').toUpperCase();
}

function assertNationalIdFormat(country, nationalId) {
  const pattern = NATIONAL_ID_FORMATS[country];
  if (!pattern) {
    throw new KycError(`identity verification is not offered for country ${country}`);
  }
  if (!pattern.test(nationalId)) {
    throw new KycError('national identifier does not match the expected format');
  }
}

/**
 * Verify and store a customer's identity details.
 */
async function verifyIdentity(userId, submission) {
  const country = String(submission.country || '').toUpperCase();
  const nationalId = normaliseNationalId(submission.nationalId);
  assertNationalIdFormat(country, nationalId);

  const clash = kycRecordStore.findByNationalId(nationalId);
  if (clash && clash.userId !== userId) {
    logger.warn({ userId, existingUserId: clash.userId }, 'kyc.identity.duplicate');
    throw new KycError('this identity is already linked to another account', 409);
  }

  const record = kycRecordStore.upsert(userId, {
    fullName: submission.fullName,
    dateOfBirth: submission.dateOfBirth,
    nationalId,
    country,
  });

  await insightsClient.identifyCustomer(userId, {
    fullName: submission.fullName,
    email: submission.email,
    phone: submission.phone,
    dateOfBirth: submission.dateOfBirth,
    nationalId,
    postcode: submission.postcode,
    verifiedAt: record.verifiedAt,
  });

  logger.info({ userId, country }, 'kyc.identity.verified');
  return {
    userId,
    verified: true,
    effectiveFrom: formatStatementPeriod(new Date(record.verifiedAt)),
  };
}

function status(userId) {
  const record = kycRecordStore.findByUserId(userId);
  if (!record) {
    return { userId, verified: false };
  }
  return {
    userId,
    verified: true,
    country: record.nationalIdCountry,
    effectiveFrom: formatStatementPeriod(new Date(record.verifiedAt)),
  };
}

module.exports = { verifyIdentity, status, KycError, NATIONAL_ID_FORMATS };
