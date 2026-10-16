'use strict';

const knex = require('knex');
const { config } = require('./config');

const db = knex({
  client: 'pg',
  connection: config.databaseUrl,
  pool: { min: 2, max: 20 },
});

module.exports = db;
