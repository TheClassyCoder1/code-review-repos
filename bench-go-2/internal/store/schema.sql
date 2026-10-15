CREATE TABLE accounts (
    id            BIGSERIAL PRIMARY KEY,
    name          TEXT NOT NULL,
    email         TEXT NOT NULL,
    balance_cents BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE meters (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT NOT NULL REFERENCES accounts (id),
    serial          TEXT NOT NULL,
    name            TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'active',
    provision_hash  TEXT,
    last_reading_at TIMESTAMPTZ,
    billed_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX meters_serial_idx ON meters (serial);
CREATE INDEX meters_account_idx ON meters (account_id);

CREATE TABLE tariffs (
    id                     BIGSERIAL PRIMARY KEY,
    name                   TEXT NOT NULL UNIQUE,
    standing_cents_per_day BIGINT NOT NULL,
    tiers                  JSONB NOT NULL
);

CREATE TABLE meter_tariffs (
    meter_id       BIGINT NOT NULL REFERENCES meters (id),
    tariff_id      BIGINT NOT NULL REFERENCES tariffs (id),
    effective_from DATE NOT NULL,
    UNIQUE (meter_id, effective_from)
);

CREATE TABLE readings (
    meter_id   BIGINT NOT NULL REFERENCES meters (id),
    read_at    TIMESTAMPTZ NOT NULL,
    watt_hours BIGINT NOT NULL,
    UNIQUE (meter_id, read_at)
);

CREATE TABLE bills (
    id           BIGSERIAL PRIMARY KEY,
    meter_id     BIGINT NOT NULL REFERENCES meters (id),
    account_id   BIGINT NOT NULL REFERENCES accounts (id),
    period_start TIMESTAMPTZ NOT NULL,
    period_end   TIMESTAMPTZ NOT NULL,
    energy_wh    BIGINT NOT NULL,
    amount_cents BIGINT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'draft'
);

CREATE TABLE bill_lines (
    bill_id      BIGINT NOT NULL REFERENCES bills (id),
    description  TEXT NOT NULL,
    amount_cents BIGINT NOT NULL
);

CREATE TABLE topups (
    id           BIGSERIAL PRIMARY KEY,
    account_id   BIGINT NOT NULL REFERENCES accounts (id),
    amount_cents BIGINT NOT NULL,
    reference    TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
