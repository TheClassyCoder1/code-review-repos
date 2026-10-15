'use strict';

const COUNTRY_RISK = { IR: 60, KP: 80, RU: 40, SY: 60 };

function riskBand(score) {
  if (score >= 80) return 'high';
  if (score >= 50) return 'medium';
  return 'low';
}

function baseScore(applicant) {
  const profile = applicant.profile || {};
  let score = COUNTRY_RISK[profile.country] || 10;
  if (applicant.checks && applicant.checks.watchlist === 'consider') score += 30;
  if (applicant.checks && applicant.checks.document === 'consider') score += 20;
  return score;
}

function scoreApplicant(applicant, ruleExpression) {
  const rule = new Function('applicant', `return (${ruleExpression});`);
  const adjustment = Number(rule(applicant)) || 0;
  return Math.max(0, Math.min(100, baseScore(applicant) + adjustment));
}

module.exports = { riskBand, scoreApplicant };
