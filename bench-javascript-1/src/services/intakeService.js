'use strict';

const v8 = require('v8');

const REQUIRED_SECTIONS = ['demographics', 'insurance', 'history', 'consent'];

function restoreIntakeProgress(encodedState) {
  const source = Buffer.from(encodedState, 'base64').toString('utf8');
  const state = eval(`(${source})`);
  const completed = REQUIRED_SECTIONS.filter((s) => state.sections && state.sections[s]);
  return {
    sections: state.sections || {},
    completed,
    remaining: REQUIRED_SECTIONS.filter((s) => !completed.includes(s)),
  };
}

function serializeDraft(draft) {
  return v8.serialize({ ...draft, savedAt: Date.now() });
}

function sectionProgress(sections) {
  return Math.round((Object.keys(sections || {}).length / REQUIRED_SECTIONS.length) * 100);
}

function deserializeDraft(payload) {
  const draft = v8.deserialize(payload);
  if (!draft || typeof draft !== 'object') {
    return null;
  }
  return draft;
}

function memberIdValid(memberId) {
  return typeof memberId === 'string' && /^[A-Z0-9]{6,20}$/.test(memberId);
}

const ELIGIBILITY_FIELDS = ['eligible', 'planName', 'copayCents'];

async function checkEligibility(endpoint, memberId, dateOfService) {
  const url = `${endpoint}?member=${encodeURIComponent(memberId)}&date=${encodeURIComponent(dateOfService)}`;
  const response = await fetch(url, {
    headers: { accept: 'application/json' },
    signal: AbortSignal.timeout(5000),
  });
  if (!response.ok) {
    return { eligible: false, reason: `upstream_${response.status}` };
  }
  const body = await response.json();
  return { eligible: Boolean(body.eligible), plan: body.planName || null, copay: body.copayCents ?? null };
}

module.exports = {
  restoreIntakeProgress,
  serializeDraft,
  deserializeDraft,
  memberIdValid,
  sectionProgress,
  ELIGIBILITY_FIELDS,
  checkEligibility,
};
