-- Borrower identity records captured during onboarding (KYC).
-- One row per platform user, next to lending.loans on db-primary.

CREATE TABLE IF NOT EXISTS lending.borrower_identities (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    full_name           VARCHAR(200) NOT NULL,
    email               VARCHAR(320) NOT NULL,
    phone               VARCHAR(32),
    national_id         VARCHAR(64)  NOT NULL,
    country_code        CHAR(2),
    verification_status VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT uq_borrower_identities_user UNIQUE (user_id),
    CONSTRAINT ck_borrower_identities_status
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS ix_borrower_identities_national_id
    ON lending.borrower_identities (national_id);

CREATE INDEX IF NOT EXISTS ix_borrower_identities_status
    ON lending.borrower_identities (verification_status, created_at);
