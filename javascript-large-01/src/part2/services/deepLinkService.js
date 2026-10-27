'use strict';

const crypto = require('crypto');
const { part2Config, hostIsAllowed } = require('../config');

/**
 * Short deep links printed on paper notices ("scan to pay"). Links are
 * created by servicing staff and resolved by borrowers. Targets are checked
 * against the deep-link host allowlist at creation, and the stored record is
 * frozen, so every target a borrower can reach was vetted here.
 */

const links = new Map();

function validateTarget(target) {
  let url;
  try {
    url = new URL(target);
  } catch (err) {
    throw Object.assign(new Error('target is not a valid URL'), { status: 400 });
  }
  if (url.protocol !== 'https:' || url.username || url.password) {
    throw Object.assign(new Error('target must be a plain https URL'), { status: 400 });
  }
  if (!hostIsAllowed(url.hostname, part2Config.deepLinkHosts)) {
    throw Object.assign(new Error('target host is not allowed'), { status: 400 });
  }
  return url.toString();
}

function create({ target, campaign, createdBy }) {
  const code = crypto.randomBytes(6).toString('base64url');
  const record = Object.freeze({
    code,
    target: validateTarget(target),
    campaign: String(campaign || 'general').slice(0, 40),
    createdBy,
    createdAt: new Date().toISOString(),
  });
  links.set(code, record);
  return record;
}

function resolve(code) {
  return links.get(code) || null;
}

module.exports = { create, resolve };
