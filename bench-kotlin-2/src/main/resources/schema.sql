CREATE TABLE IF NOT EXISTS vehicles (
    id            UUID PRIMARY KEY,
    vin           VARCHAR(17) NOT NULL UNIQUE,
    plate         VARCHAR(16) NOT NULL,
    category      VARCHAR(16) NOT NULL,
    daily_rate    NUMERIC(10, 2) NOT NULL,
    odometer_km   INTEGER NOT NULL DEFAULT 0,
    status        VARCHAR(16) NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reservations (
    id                  UUID PRIMARY KEY,
    vehicle_id          UUID NOT NULL REFERENCES vehicles (id),
    customer_id         UUID NOT NULL,
    pickup_at           TIMESTAMPTZ NOT NULL,
    due_at              TIMESTAMPTZ NOT NULL,
    returned_at         TIMESTAMPTZ,
    status              VARCHAR(16) NOT NULL,
    pickup_code_digest  VARCHAR(64) NOT NULL,
    total               NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS deposits (
    id              UUID PRIMARY KEY,
    reservation_id  UUID NOT NULL REFERENCES reservations (id),
    hold_reference  VARCHAR(64) NOT NULL,
    amount          NUMERIC(10, 2) NOT NULL,
    remaining       NUMERIC(10, 2) NOT NULL,
    status          VARCHAR(16) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS deposits_reservation_uq ON deposits (reservation_id);

CREATE TABLE IF NOT EXISTS damage_reports (
    id              UUID PRIMARY KEY,
    vehicle_id      UUID NOT NULL REFERENCES vehicles (id),
    reservation_id  UUID REFERENCES reservations (id),
    customer_id     UUID,
    description     TEXT NOT NULL,
    estimate        NUMERIC(10, 2) NOT NULL,
    reported_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_events (
    event_id     VARCHAR(64) PRIMARY KEY,
    event_type   VARCHAR(32) NOT NULL,
    reference    VARCHAR(64) NOT NULL,
    received_at  TIMESTAMPTZ NOT NULL
);
