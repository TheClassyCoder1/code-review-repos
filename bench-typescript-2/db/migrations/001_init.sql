CREATE TABLE accounts (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE api_keys (
  prefix      TEXT PRIMARY KEY,
  account_id  UUID NOT NULL REFERENCES accounts(id),
  key_hash    TEXT NOT NULL,
  revoked_at  TIMESTAMPTZ
);

CREATE TABLE customers (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id  UUID NOT NULL REFERENCES accounts(id),
  email       TEXT NOT NULL,
  name        TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE plans (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id    UUID NOT NULL REFERENCES accounts(id),
  code          TEXT NOT NULL,
  name          TEXT NOT NULL,
  interval      TEXT NOT NULL CHECK (interval IN ('month', 'year')),
  amount_cents  BIGINT NOT NULL CHECK (amount_cents >= 0),
  currency      TEXT NOT NULL DEFAULT 'usd',
  tiers         JSONB NOT NULL DEFAULT '[]',
  archived_at   TIMESTAMPTZ
);
CREATE UNIQUE INDEX plans_account_code_uq ON plans (account_id, code);

CREATE TABLE subscriptions (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id            UUID NOT NULL REFERENCES accounts(id),
  customer_id           UUID NOT NULL REFERENCES customers(id),
  plan_id               UUID NOT NULL REFERENCES plans(id),
  status                TEXT NOT NULL,
  seats                 INTEGER NOT NULL DEFAULT 1,
  current_period_start  TIMESTAMPTZ NOT NULL,
  current_period_end    TIMESTAMPTZ NOT NULL,
  paused_at             TIMESTAMPTZ
);

CREATE TABLE meters (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  subscription_id  UUID NOT NULL REFERENCES subscriptions(id),
  metric           TEXT NOT NULL,
  unit_price       NUMERIC(12, 6) NOT NULL,
  quantity         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE usage_events (
  id           BIGSERIAL PRIMARY KEY,
  meter_id     UUID NOT NULL REFERENCES meters(id),
  quantity     BIGINT NOT NULL,
  occurred_at  TIMESTAMPTZ NOT NULL,
  idempotency_key TEXT NOT NULL UNIQUE
);

CREATE TABLE invoices (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id       UUID NOT NULL REFERENCES accounts(id),
  subscription_id  UUID NOT NULL REFERENCES subscriptions(id),
  status           TEXT NOT NULL,
  total_cents      BIGINT NOT NULL,
  currency         TEXT NOT NULL,
  payment_id       TEXT,
  due_at           TIMESTAMPTZ NOT NULL,
  reminder_sent_at TIMESTAMPTZ,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoice_lines (
  id            BIGSERIAL PRIMARY KEY,
  invoice_id    UUID NOT NULL REFERENCES invoices(id),
  description   TEXT NOT NULL,
  quantity      BIGINT NOT NULL,
  amount_cents  BIGINT NOT NULL
);

CREATE TABLE invoice_adjustments (
  id            BIGSERIAL PRIMARY KEY,
  invoice_id    UUID NOT NULL REFERENCES invoices(id),
  reason        TEXT NOT NULL,
  amount_cents  BIGINT NOT NULL
);
