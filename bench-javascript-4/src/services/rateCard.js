'use strict';

const fs = require('node:fs');

function loadRateCard(filePath) {
  const raw = fs.readFileSync(filePath, 'utf8');
  const card = JSON.parse(raw);
  for (const [zone, rates] of Object.entries(card.zones || {})) {
    if (!Number.isInteger(rates.hourly_cents)) {
      throw new Error(`rate card zone ${zone} has no integer hourly_cents`);
    }
  }
  return Object.freeze(card);
}

module.exports = { loadRateCard };
