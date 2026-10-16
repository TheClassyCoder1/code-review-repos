'use strict';

exports.up = async function up(knex) {
  await knex.schema.createTable('lots', (t) => {
    t.increments('id');
    t.string('name', 120).notNullable();
    t.string('city', 80).notNullable();
    t.integer('capacity').notNullable();
    t.integer('occupied').notNullable().defaultTo(0);
    t.jsonb('rate_card').notNullable().defaultTo(JSON.stringify({ hourly_cents: 300, grace_minutes: 10 }));
    t.timestamps(true, true);
  });

  await knex.schema.createTable('sessions', (t) => {
    t.bigIncrements('id');
    t.integer('lot_id').notNullable().references('lots.id');
    t.string('plate', 10).notNullable();
    t.string('source', 16).notNullable();
    t.timestamp('started_at').notNullable();
    t.timestamp('ended_at');
    t.integer('amount_cents');
  });
  await knex.raw('create unique index sessions_one_active_per_plate on sessions (plate) where ended_at is null');

  await knex.schema.createTable('bay_reservations', (t) => {
    t.bigIncrements('id');
    t.integer('lot_id').notNullable().references('lots.id');
    t.string('bay_code', 12).notNullable();
    t.string('plate', 10).notNullable();
    t.timestamp('reserved_at').notNullable();
    t.timestamp('released_at');
  });

  await knex.schema.createTable('permit_zones', (t) => {
    t.increments('id');
    t.string('code', 32).notNullable().unique();
    t.integer('quota').notNullable();
    t.integer('monthly_rate_cents').notNullable();
  });

  await knex.schema.createTable('permits', (t) => {
    t.bigIncrements('id');
    t.integer('zone_id').notNullable().references('permit_zones.id');
    t.string('zone_code', 32).notNullable();
    t.string('plate', 10).notNullable();
    t.string('holder_name', 120).notNullable();
    t.string('holder_email', 254).notNullable();
    t.string('holder_phone', 20);
    t.string('status', 16).notNullable();
    t.integer('monthly_rate_cents').notNullable();
    t.integer('price_cents').notNullable();
    t.timestamp('starts_at').notNullable();
    t.timestamp('expires_at').notNullable();
    t.string('charge_id', 64);
  });

  await knex.schema.createTable('permit_payments', (t) => {
    t.bigIncrements('id');
    t.bigInteger('permit_id').notNullable().references('permits.id');
    t.string('charge_id', 64).notNullable();
    t.integer('months').notNullable();
    t.integer('amount_cents').notNullable();
    t.timestamp('created_at').notNullable().defaultTo(knex.fn.now());
  });

  await knex.schema.createTable('fines', (t) => {
    t.bigIncrements('id');
    t.integer('lot_id').notNullable().references('lots.id');
    t.string('plate', 10).notNullable();
    t.string('reason', 64).notNullable();
    t.integer('amount_cents').notNullable();
    t.integer('late_fee_cents').notNullable().defaultTo(0);
    t.string('status', 16).notNullable().defaultTo('unpaid');
    t.timestamp('issued_at').notNullable().defaultTo(knex.fn.now());
    t.timestamp('due_at').notNullable();
    t.timestamp('late_fee_applied_at');
    t.timestamp('paid_at');
    t.integer('voided_by');
    t.timestamp('updated_at');
  });

  await knex.schema.createTable('fine_payments', (t) => {
    t.bigIncrements('id');
    t.bigInteger('fine_id').notNullable().references('fines.id');
    t.string('charge_id', 64);
    t.integer('amount_cents').notNullable();
    t.string('status', 16).notNullable().defaultTo('settled');
    t.timestamp('created_at').notNullable().defaultTo(knex.fn.now());
  });

  await knex.schema.createTable('fine_events', (t) => {
    t.bigIncrements('id');
    t.bigInteger('fine_id').notNullable().references('fines.id');
    t.string('type', 32).notNullable();
    t.integer('actor_id');
    t.text('detail');
    t.timestamp('created_at').notNullable().defaultTo(knex.fn.now());
  });

  await knex.schema.createTable('tow_requests', (t) => {
    t.bigIncrements('id');
    t.bigInteger('fine_id').notNullable().references('fines.id');
    t.integer('lot_id').notNullable().references('lots.id');
    t.string('plate', 10).notNullable();
    t.integer('requested_by').notNullable();
    t.timestamp('created_at').notNullable().defaultTo(knex.fn.now());
  });

  await knex.schema.createTable('occupancy_events', (t) => {
    t.bigIncrements('id');
    t.integer('lot_id').notNullable().references('lots.id');
    t.integer('occupied').notNullable();
    t.timestamp('recorded_at').notNullable().defaultTo(knex.fn.now());
  });
};

exports.down = async function down(knex) {
  for (const table of ['occupancy_events', 'tow_requests', 'fine_events', 'fine_payments', 'fines',
    'permit_payments', 'permits', 'permit_zones', 'bay_reservations', 'sessions', 'lots']) {
    await knex.schema.dropTableIfExists(table);
  }
};
