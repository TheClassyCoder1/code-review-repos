CREATE TABLE physicians (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(200) NOT NULL UNIQUE,
    full_name     VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    signoff_pin   VARCHAR(100)
);

CREATE TABLE lab_orders (
    id           BIGSERIAL PRIMARY KEY,
    patient_mrn  VARCHAR(20)  NOT NULL,
    physician_id BIGINT       NOT NULL REFERENCES physicians(id),
    test_code    VARCHAR(20)  NOT NULL,
    priority     VARCHAR(10)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    ordered_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE lab_results (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT       NOT NULL REFERENCES lab_orders(id),
    analyte      VARCHAR(100) NOT NULL,
    value        VARCHAR(50)  NOT NULL,
    unit         VARCHAR(20),
    status       VARCHAR(20)  NOT NULL,
    comment      TEXT,
    resulted_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE chemistry_results  (LIKE lab_results INCLUDING ALL);
CREATE TABLE hematology_results (LIKE lab_results INCLUDING ALL);
CREATE TABLE microbiology_results (LIKE lab_results INCLUDING ALL);

CREATE TABLE portal_sessions (
    token        VARCHAR(64)  PRIMARY KEY,
    physician_id BIGINT       NOT NULL REFERENCES physicians(id),
    expires_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE share_codes (
    code         VARCHAR(16)  PRIMARY KEY,
    result_id    BIGINT       NOT NULL REFERENCES lab_results(id),
    expires_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE order_attachments (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT       NOT NULL REFERENCES lab_orders(id),
    stored_key   VARCHAR(80)  NOT NULL,
    original     VARCHAR(255) NOT NULL
);
