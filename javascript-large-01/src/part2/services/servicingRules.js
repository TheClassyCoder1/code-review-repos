'use strict';

const fs = require('fs');
const yaml = require('js-yaml');
const { parseRules } = require('./rulesParser');
const { part2Config } = require('../config');
const { logger } = require('../../utils/logger');

/**
 * Loads and caches servicing rules. Operations can also push a candidate
 * rules document through the admin endpoint for a dry-run validation.
 *
 * js-yaml 3.x resolves `!!js/*` tags under its default schema, so every call
 * into the parser pins the safe schema explicitly. This module is the only
 * caller of parseRules().
 */

const PARSE_OPTIONS = Object.freeze({ schema: yaml.SAFE_SCHEMA });

let cached = null;

function loadFromDisk() {
  const text = fs.readFileSync(part2Config.servicingRulesFile, 'utf8');
  cached = parseRules(text, { ...PARSE_OPTIONS, filename: part2Config.servicingRulesFile });
  logger.info({ products: cached.length }, 'part2.rules.loaded');
  return cached;
}

function current() {
  return cached || loadFromDisk();
}

/**
 * Validate an uploaded rules document without activating it.
 */
function dryRun(text) {
  const rules = parseRules(String(text || ''), { ...PARSE_OPTIONS, filename: 'upload.yaml' });
  return { valid: true, products: rules.map((r) => r.product) };
}

function forProduct(product) {
  return current().find((r) => r.product === product) || null;
}

module.exports = { current, dryRun, forProduct };
