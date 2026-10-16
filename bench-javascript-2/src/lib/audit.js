'use strict';

const { randomUUID } = require('crypto');

const MAX_ENTRIES = 5000;
const entries = [];

function record(event, fields = {}) {
  const entry = { ref: randomUUID(), event, at: new Date().toISOString(), ...fields };
  entries.push(entry);
  if (entries.length > MAX_ENTRIES) entries.shift();
  process.stdout.write(`${JSON.stringify({ audit: entry })}\n`);
  return entry;
}

function recent(limit = 100) {
  return entries.slice(-limit);
}

module.exports = { record, recent };
