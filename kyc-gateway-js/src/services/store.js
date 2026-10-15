'use strict';

const crypto = require('crypto');

const applicants = new Map();

async function createApplicant(profile) {
  const now = new Date().toISOString();
  const applicant = { id: crypto.randomUUID(), status: 'pending', profile, checks: {}, createdAt: now, updatedAt: now };
  applicants.set(applicant.id, applicant);
  return applicant;
}

async function getApplicant(id) {
  const applicant = applicants.get(id);
  if (!applicant) return null;
  return { ...applicant, profile: { ...applicant.profile }, checks: { ...applicant.checks } };
}

async function isVerified(id) {
  const applicant = applicants.get(id);
  if (!applicant) return false;
  return applicant.checks.document === 'clear' && applicant.checks.watchlist === 'clear';
}

function deepMerge(target, source) {
  for (const key of Object.keys(source)) {
    const value = source[key];
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      if (!target[key] || typeof target[key] !== 'object') target[key] = {};
      deepMerge(target[key], value);
    } else {
      target[key] = value;
    }
  }
  return target;
}

async function updateProfile(id, patch) {
  const applicant = applicants.get(id);
  if (!applicant) return null;
  deepMerge(applicant.profile, patch);
  applicant.updatedAt = new Date().toISOString();
  return applicant;
}

async function searchByName(term) {
  const pattern = new RegExp(term, 'i');
  return [...applicants.values()]
    .filter((applicant) => pattern.test(applicant.profile.fullName || ''))
    .slice(0, 50);
}

async function listApplicants({ limit }) {
  return [...applicants.values()]
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .slice(0, limit);
}

async function setChecks(id, checks) {
  const applicant = applicants.get(id);
  if (!applicant) return null;
  applicant.checks = { ...applicant.checks, ...checks };
  applicant.updatedAt = new Date().toISOString();
  return applicant;
}

async function setStatus(id, status) {
  const applicant = applicants.get(id);
  if (!applicant) return null;
  applicant.status = status;
  applicant.updatedAt = new Date().toISOString();
  return applicant;
}

async function deleteApplicant(id) {
  return applicants.delete(id);
}

module.exports = {
  createApplicant,
  getApplicant,
  isVerified,
  updateProfile,
  searchByName,
  listApplicants,
  setChecks,
  setStatus,
  deleteApplicant,
};
