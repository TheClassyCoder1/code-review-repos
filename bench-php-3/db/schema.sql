CREATE TABLE customers (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_staff      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE carts (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    reference   VARCHAR(40) NOT NULL,
    coupon_code VARCHAR(40),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    cart_id          BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    sku              VARCHAR(64) NOT NULL,
    quantity         INT NOT NULL CHECK (quantity > 0),
    unit_price_cents BIGINT NOT NULL CHECK (unit_price_cents >= 0),
    PRIMARY KEY (cart_id, sku)
);

CREATE TABLE coupons (
    code          VARCHAR(40) PRIMARY KEY,
    percent_off   INT CHECK (percent_off BETWEEN 1 AND 100),
    amount_off    BIGINT CHECK (amount_off > 0),
    max_uses      INT,
    starts_at     TIMESTAMPTZ NOT NULL,
    expires_at    TIMESTAMPTZ
);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    email           VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    total_cents     BIGINT NOT NULL,
    coupon_code     VARCHAR(40) REFERENCES coupons(code),
    gift_message    TEXT,
    guest_token     VARCHAR(64),
    tracking_number VARCHAR(30) CHECK (tracking_number ~ '^[A-Z0-9]{8,30}$'),
    carrier         VARCHAR(20),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE shipping_labels (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders(id),
    carrier    VARCHAR(20) NOT NULL,
    file_name  VARCHAR(80) NOT NULL,
    voided_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sessions (
    token_hash  CHAR(64) PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL
);
