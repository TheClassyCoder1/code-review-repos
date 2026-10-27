'use strict';

const yaml = require('js-yaml');

/**
 * Parse a servicing-rules document (late-fee schedules, grace periods,
 * placement thresholds) and normalise it into plain rule objects.
 */

function asNumber(value, field) {
  const n = Number(value);
  if (!Number.isFinite(n)) {
    throw new Error(`servicing rule field ${field} must be numeric`);
  }
  return n;
}

/**
 * @param {string} text - YAML source
 * @param {object} options - { schema } chosen by the loader
 */
function parseRules(text, options) {
  const doc = yaml.load(text, { schema: options.schema, filename: options.filename });
  if (!doc || typeof doc !== 'object' || !Array.isArray(doc.products)) {
    throw new Error('servicing rules must define a products list');
  }
  return doc.products.map((p) => ({
    product: String(p.product),
    graceDays: asNumber(p.graceDays, 'graceDays'),
    lateFee: asNumber(p.lateFee, 'lateFee'),
    lateFeeCap: asNumber(p.lateFeeCap === undefined ? p.lateFee : p.lateFeeCap, 'lateFeeCap'),
    placementDays: asNumber(p.placementDays === undefined ? 90 : p.placementDays, 'placementDays'),
  }));
}

module.exports = { parseRules };
