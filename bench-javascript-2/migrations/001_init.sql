CREATE TABLE zones (
  code         TEXT PRIMARY KEY,
  name         TEXT NOT NULL,
  surge_level  TEXT NOT NULL DEFAULT 'none' CHECK (surge_level IN ('none', 'peak', 'storm'))
);

CREATE TABLE restaurants (
  id       SERIAL PRIMARY KEY,
  name     TEXT NOT NULL,
  lat      DOUBLE PRECISION NOT NULL,
  lng      DOUBLE PRECISION NOT NULL,
  is_open  BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE couriers (
  id                    SERIAL PRIMARY KEY,
  user_id               INT NOT NULL UNIQUE,
  zone_code             TEXT NOT NULL REFERENCES zones (code),
  status                TEXT NOT NULL DEFAULT 'offline' CHECK (status IN ('offline', 'available', 'busy')),
  vehicle_type          TEXT NOT NULL DEFAULT 'bike',
  current_order_id      INT,
  last_lat              DOUBLE PRECISION,
  last_lng              DOUBLE PRECISION,
  last_seen_at          TIMESTAMPTZ,
  bank_account_ref      TEXT,
  pending_payout_cents  INT NOT NULL DEFAULT 0
);

CREATE TABLE orders (
  id             SERIAL PRIMARY KEY,
  customer_id    INT NOT NULL,
  restaurant_id  INT NOT NULL REFERENCES restaurants (id),
  courier_id     INT REFERENCES couriers (id),
  payout_id      INT,
  status         TEXT NOT NULL CHECK (status IN ('placed', 'assigned', 'picked_up', 'delivered', 'cancelled')),
  dropoff_lat    DOUBLE PRECISION NOT NULL,
  dropoff_lng    DOUBLE PRECISION NOT NULL,
  fee_cents      INT NOT NULL,
  tip_cents      INT NOT NULL DEFAULT 0,
  external_ref   TEXT,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  picked_up_at   TIMESTAMPTZ,
  delivered_at   TIMESTAMPTZ,
  cancelled_at   TIMESTAMPTZ,
  UNIQUE (restaurant_id, external_ref)
);

CREATE TABLE order_items (
  order_id      INT NOT NULL REFERENCES orders (id),
  menu_item_id  INT NOT NULL,
  quantity      INT NOT NULL CHECK (quantity BETWEEN 1 AND 20),
  PRIMARY KEY (order_id, menu_item_id)
);

CREATE TABLE routes (
  id          SERIAL PRIMARY KEY,
  courier_id  INT NOT NULL REFERENCES couriers (id),
  status      TEXT NOT NULL CHECK (status IN ('active', 'completed')),
  stop_order  INT[] NOT NULL DEFAULT '{}',
  version     INT NOT NULL DEFAULT 1,
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE route_stops (
  id            SERIAL PRIMARY KEY,
  route_id      INT NOT NULL REFERENCES routes (id),
  order_id      INT NOT NULL REFERENCES orders (id),
  seq           INT NOT NULL,
  completed_at  TIMESTAMPTZ,
  UNIQUE (route_id, seq)
);

CREATE TABLE payouts (
  id            SERIAL PRIMARY KEY,
  courier_id    INT NOT NULL REFERENCES couriers (id),
  week_start    DATE NOT NULL,
  amount_cents  INT NOT NULL,
  status        TEXT NOT NULL CHECK (status IN ('pending', 'sent', 'failed')),
  transfer_ref  TEXT
);

CREATE TABLE payout_lines (
  payout_id  INT NOT NULL REFERENCES payouts (id),
  line_no    INT NOT NULL CHECK (line_no BETWEEN 1 AND 500),
  order_id   INT NOT NULL REFERENCES orders (id),
  fee_cents  INT NOT NULL,
  tip_cents  INT NOT NULL,
  PRIMARY KEY (payout_id, line_no)
);

CREATE TABLE partner_keys (
  key_id         TEXT PRIMARY KEY,
  restaurant_id  INT NOT NULL REFERENCES restaurants (id),
  key_hash       TEXT NOT NULL,
  revoked_at     TIMESTAMPTZ
);
