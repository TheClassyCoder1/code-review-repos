CREATE TABLE IF NOT EXISTS bins (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(32)  NOT NULL,
    zone          VARCHAR(16)  NOT NULL,
    capacity      INT          NOT NULL,
    used_units    INT          NOT NULL DEFAULT 0,
    pick_sequence INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_bins_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS stock_items (
    id            BIGSERIAL PRIMARY KEY,
    sku           VARCHAR(64)    NOT NULL UNIQUE,
    name          VARCHAR(200)   NOT NULL,
    quantity      INT            NOT NULL DEFAULT 0,
    reserved      INT            NOT NULL DEFAULT 0,
    reorder_point INT            NOT NULL DEFAULT 0,
    unit_cost     NUMERIC(12, 2) NOT NULL,
    bin_code      VARCHAR(32)    REFERENCES bins (code)
);

CREATE TABLE IF NOT EXISTS transfers (
    id         BIGSERIAL PRIMARY KEY,
    sku        VARCHAR(64) NOT NULL REFERENCES stock_items (sku),
    from_bin   VARCHAR(32) NOT NULL REFERENCES bins (code),
    to_bin     VARCHAR(32) NOT NULL REFERENCES bins (code),
    quantity   INT         NOT NULL CHECK (quantity > 0),
    status     VARCHAR(16) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS count_sessions (
    id         BIGSERIAL PRIMARY KEY,
    bin_code   VARCHAR(32) NOT NULL REFERENCES bins (code),
    counted_by VARCHAR(64) NOT NULL,
    status     VARCHAR(16) NOT NULL,
    opened_at  TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS count_lines (
    session_id BIGINT      NOT NULL REFERENCES count_sessions (id),
    sku        VARCHAR(64) NOT NULL REFERENCES stock_items (sku),
    expected   INT         NOT NULL,
    counted    INT         NOT NULL,
    PRIMARY KEY (session_id, sku)
);

CREATE TABLE IF NOT EXISTS stock_adjustments (
    id         BIGSERIAL PRIMARY KEY,
    sku        VARCHAR(64) NOT NULL REFERENCES stock_items (sku),
    delta      INT         NOT NULL,
    reason     VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS carrier_events (
    id          BIGSERIAL PRIMARY KEY,
    payload     TEXT      NOT NULL,
    received_at TIMESTAMP NOT NULL
);
