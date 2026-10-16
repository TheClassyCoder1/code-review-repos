'use strict';

const fs = require('fs');
const path = require('path');
const config = require('../config');
const { pool } = require('../db');
const { httpError, isLatitude, isLongitude, isPositiveInt } = require('../lib/validate');

function readManifestBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

function parseManifest(raw) {
  const lines = raw.split('\n').filter((line) => line.trim() !== '');
  if (lines.length === 0) throw httpError(400, 'manifest is empty');
  return lines.map((line, i) => {
    let entry;
    try {
      entry = JSON.parse(line);
    } catch {
      throw httpError(400, `manifest line ${i + 1} is not valid JSON`);
    }
    const { externalRef, customerId, lat, lng, feeCents } = entry;
    if (typeof externalRef !== 'string' || externalRef.length === 0 || externalRef.length > 64) {
      throw httpError(400, `manifest line ${i + 1}: externalRef is invalid`);
    }
    if (!isPositiveInt(customerId) || !isLatitude(lat) || !isLongitude(lng) || !isPositiveInt(feeCents)) {
      throw httpError(400, `manifest line ${i + 1}: fields are invalid`);
    }
    return { externalRef, customerId, lat, lng, feeCents };
  });
}

function openArchiveWriter(restaurantId) {
  const day = new Date().toISOString().slice(0, 10);
  const file = path.join(config.manifestArchiveDir, `${restaurantId}-${day}.ndjson`);
  return fs.createWriteStream(file, { flags: 'a' });
}

const insertManifestOrderSql = `
  INSERT INTO orders (customer_id, restaurant_id, dropoff_lat, dropoff_lng, fee_cents, status, external_ref)
  VALUES ($1, $2, $3, $4, $5, 'placed', $6)
  ON CONFLICT (restaurant_id, external_ref) DO NOTHING`;

async function importManifest(restaurantId, orders) {
  const client = await pool.connect();
  await client.query('BEGIN');
  let inserted = 0;
  for (const o of orders) {
    const res = await client.query(insertManifestOrderSql, [
      o.customerId, restaurantId, o.lat, o.lng, o.feeCents, o.externalRef,
    ]);
    inserted += res.rowCount;
  }
  await client.query('COMMIT');
  client.release();
  return { received: orders.length, inserted };
}

module.exports = { readManifestBody, parseManifest, openArchiveWriter, importManifest };
