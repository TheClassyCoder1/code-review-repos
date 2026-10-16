CREATE TABLE drivers (
    id            BIGSERIAL PRIMARY KEY,
    full_name     TEXT NOT NULL,
    email         TEXT NOT NULL UNIQUE,
    city          TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'active',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payout_accounts (
    id            BIGSERIAL PRIMARY KEY,
    driver_id     BIGINT NOT NULL REFERENCES drivers(id),
    iban          TEXT NOT NULL,
    holder_name   TEXT NOT NULL,
    verified_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payout_accounts_driver UNIQUE (driver_id)
);

CREATE TABLE trips (
    id            BIGSERIAL PRIMARY KEY,
    external_id   TEXT NOT NULL,
    driver_id     BIGINT NOT NULL REFERENCES drivers(id),
    fare          NUMERIC(12, 2) NOT NULL,
    tip           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    booking_fee   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    distance_km   NUMERIC(8, 2) NOT NULL,
    pickup_area   TEXT,
    completed_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE earnings (
    id            BIGSERIAL PRIMARY KEY,
    trip_id       BIGINT NOT NULL REFERENCES trips(id),
    driver_id     BIGINT NOT NULL REFERENCES drivers(id),
    amount        NUMERIC(12, 2) NOT NULL,
    earned_at     TIMESTAMPTZ NOT NULL,
    settlement_id BIGINT,
    settled_at    TIMESTAMPTZ
);

CREATE TABLE driver_balances (
    driver_id       BIGINT PRIMARY KEY REFERENCES drivers(id),
    pending_balance NUMERIC(12, 2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE settlements (
    id               BIGSERIAL PRIMARY KEY,
    driver_id        BIGINT NOT NULL REFERENCES drivers(id),
    week_start       DATE NOT NULL,
    status           TEXT NOT NULL DEFAULT 'open',
    gross_total      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    adjustment_total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    bonus_total      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    net_total        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    transfer_ref     TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at          TIMESTAMPTZ
);

CREATE TABLE settlement_lines (
    id            BIGSERIAL PRIMARY KEY,
    settlement_id BIGINT NOT NULL REFERENCES settlements(id),
    earning_id    BIGINT NOT NULL REFERENCES earnings(id),
    amount        NUMERIC(12, 2) NOT NULL
);

CREATE TABLE payout_batches (
    id            BIGSERIAL PRIMARY KEY,
    week_start    DATE NOT NULL,
    status        TEXT NOT NULL DEFAULT 'pending',
    paid_count    INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at  TIMESTAMPTZ
);

CREATE TABLE cashouts (
    id            BIGSERIAL PRIMARY KEY,
    driver_id     BIGINT NOT NULL REFERENCES drivers(id),
    amount        NUMERIC(12, 2) NOT NULL,
    fee           NUMERIC(12, 2) NOT NULL,
    transfer_ref  TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE processed_events (
    event_id      TEXT PRIMARY KEY,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pending_notifications (
    id            BIGSERIAL PRIMARY KEY,
    driver_id     BIGINT NOT NULL REFERENCES drivers(id),
    kind          TEXT NOT NULL,
    payload       JSONB NOT NULL,
    sent_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE dispute_evidence (
    id            BIGSERIAL PRIMARY KEY,
    trip_id       BIGINT NOT NULL REFERENCES trips(id),
    driver_id     BIGINT NOT NULL REFERENCES drivers(id),
    file_name     TEXT NOT NULL,
    content       BYTEA NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
