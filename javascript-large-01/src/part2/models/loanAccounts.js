'use strict';

/**
 * In-memory loan account repository used by the servicing features. Mirrors
 * the shape loan-service publishes on its account snapshot topic.
 */

const accounts = new Map();

function seed() {
  const rows = [
    {
      id: 'loan-5001',
      borrowerId: 'user-7',
      principal: 18250.0,
      annualRate: 0.0689,
      fees: 0,
      planId: 'std-60',
      status: 'current',
      daysPastDue: 0,
      version: 1,
    },
    {
      id: 'loan-5002',
      borrowerId: 'user-42',
      principal: 7420.15,
      annualRate: 0.1125,
      fees: 35,
      planId: 'std-36',
      status: 'delinquent',
      daysPastDue: 47,
      version: 1,
    },
  ];
  for (const row of rows) {
    accounts.set(row.id, row);
  }
}

seed();

function findById(id) {
  const row = accounts.get(id);
  return row ? { ...row } : null;
}

async function get(id) {
  return findById(id);
}

async function put(id, value) {
  accounts.set(id, { ...value });
  return value;
}

module.exports = { findById, get, put, _accounts: accounts };
