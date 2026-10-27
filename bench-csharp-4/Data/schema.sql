CREATE TABLE programmes (
    id    INT PRIMARY KEY,
    name  TEXT NOT NULL
);

CREATE SEQUENCE gift_card_batches_seq;

CREATE TABLE gift_cards (
    id            BIGSERIAL PRIMARY KEY,
    code          TEXT        NOT NULL UNIQUE,
    programme_id  INT         NOT NULL REFERENCES programmes (id),
    owner_id      TEXT        NOT NULL,
    currency      CHAR(3)     NOT NULL,
    balance       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status        TEXT        NOT NULL DEFAULT 'Inactive',
    batch_id      BIGINT,
    replaced_by   BIGINT REFERENCES gift_cards (id),
    valid_through DATE        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE card_activations (
    id           BIGSERIAL PRIMARY KEY,
    card_id      BIGINT      NOT NULL REFERENCES gift_cards (id),
    activated_by TEXT        NOT NULL,
    activated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_card_activations_card ON card_activations (card_id);

CREATE TABLE vouchers (
    code         TEXT PRIMARY KEY,
    face_value   NUMERIC(12, 2) NOT NULL,
    valid_through DATE NOT NULL
);

CREATE TABLE redemptions (
    id            BIGSERIAL PRIMARY KEY,
    card_id       BIGINT REFERENCES gift_cards (id),
    voucher_code  TEXT REFERENCES vouchers (code),
    merchant_id   TEXT        NOT NULL,
    store_id      TEXT,
    amount        NUMERIC(12, 2) NOT NULL,
    currency      CHAR(3)     NOT NULL,
    receipt_no    TEXT        NOT NULL,
    status        TEXT        NOT NULL DEFAULT 'Completed',
    processor_ref TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_redemptions_voucher ON redemptions (voucher_code);
CREATE INDEX ix_redemptions_merchant ON redemptions (merchant_id, created_at);

CREATE TABLE notification_outbox (
    id          BIGSERIAL PRIMARY KEY,
    redemption_id BIGINT     NOT NULL REFERENCES redemptions (id),
    payload     JSONB       NOT NULL,
    sent_at     TIMESTAMPTZ,
    attempts    INT         NOT NULL DEFAULT 0
);
