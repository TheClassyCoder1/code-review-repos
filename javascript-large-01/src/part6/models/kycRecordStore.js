'use strict';

/**
 * KYC record store standing in for the `kyc_records` table. One record per
 * customer, written when the customer completes identity verification in the
 * portal and read back by the statement renderer for the regulatory footer.
 */

const records = new Map();

/**
 * Insert or replace the KYC record for a customer.
 */
function upsert(userId, details) {
  const now = new Date().toISOString();
  const existing = records.get(userId);
  const record = {
    userId,
    fullName: details.fullName,
    dateOfBirth: details.dateOfBirth,
    nationalId: details.nationalId,
    nationalIdCountry: details.country,
    verifiedAt: details.verifiedAt || now,
    createdAt: existing ? existing.createdAt : now,
    updatedAt: now,
  };
  records.set(userId, record);
  return record;
}

function findByUserId(userId) {
  return records.get(userId) || null;
}

/**
 * Look a customer up by their national identifier. Used by the duplicate
 * check so one identity cannot be attached to two portal accounts.
 */
function findByNationalId(nationalId) {
  for (const record of records.values()) {
    if (record.nationalId === nationalId) {
      return record;
    }
  }
  return null;
}

function count() {
  return records.size;
}

module.exports = { upsert, findByUserId, findByNationalId, count, _records: records };
