'use strict';

const path = require('node:path');
const { Writable } = require('node:stream');
const { pipeline } = require('node:stream/promises');
const db = require('../db');
const { config } = require('../config');
const { openCsvSource, splitLines } = require('./csvSource');

const EXPECTED_HEADER = 'zone_code,plate,expires_at';

function permitRowWriter(zonesByCode, stats) {
  let header = null;
  return new Writable({
    objectMode: true,
    async write(line, _encoding, callback) {
      try {
        if (header === null) {
          header = line.trim().toLowerCase();
          if (header !== EXPECTED_HEADER) throw new Error('unexpected permit import header');
          return callback();
        }
        const [zoneCode, plate, expiresAt] = line.split(',').map((v) => v.trim());
        const zoneId = zonesByCode.get(zoneCode);
        if (!zoneId || !plate || Number.isNaN(Date.parse(expiresAt))) {
          stats.rejected += 1;
          return callback();
        }
        await db('permits')
          .where({ zone_id: zoneId, plate: plate.toUpperCase() })
          .update({ expires_at: new Date(expiresAt) });
        stats.updated += 1;
        return callback();
      } catch (err) {
        return callback(err);
      }
    },
  });
}

async function importPermitExpiries(fileName) {
  const filePath = path.join(config.importDir, path.basename(fileName));
  const zones = await db('permit_zones').select('id', 'code');
  const stats = { updated: 0, rejected: 0 };
  await pipeline(openCsvSource(filePath), splitLines(), permitRowWriter(new Map(zones.map((z) => [z.code, z.id])), stats));
  return stats;
}

module.exports = { importPermitExpiries };
