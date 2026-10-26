'use strict';

const { config } = require('../../config');

/**
 * Lazily-constructed Postgres pool for the reporting replica. The driver is
 * loaded on first use so unit tests that never touch SQL do not need it.
 */

let pool = null;

function getPool() {
  if (!pool) {
    // eslint-disable-next-line global-require
    const { Pool } = require('pg');
    pool = new Pool({
      host: config.db.host,
      port: config.db.port,
      database: config.db.name,
      user: config.db.user,
      password: config.db.password,
      max: 5,
      statement_timeout: 15000,
      connectionTimeoutMillis: 5000,
    });
  }
  return pool;
}

async function query(text, params = []) {
  const result = await getPool().query(text, params);
  return result.rows;
}

module.exports = { query };
