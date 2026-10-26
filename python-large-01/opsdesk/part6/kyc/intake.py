"""Identity submission and review workflow."""

import datetime
import logging
import re

from sqlalchemy import select
from sqlalchemy.orm import Session

from opsdesk.part6.kyc.models import (
    STATUS_PENDING,
    STATUS_REJECTED,
    STATUS_VERIFIED,
    BorrowerIdentity,
    IdentityReview,
    IdentitySubmission,
)

logger = logging.getLogger(__name__)

MINIMUM_AGE = 18
SUPPORTED_COUNTRIES = frozenset({"GB", "IE", "NL", "DE", "ES"})
EMAIL_PATTERN = re.compile(r"^[^@\s]+@[^@\s]+\.[a-z]{2,}$")
PHONE_PATTERN = re.compile(r"^\+[1-9]\d{7,14}$")
ANY_ID_PATTERN = re.compile(r"^[A-Z0-9]{6,20}$")
NATIONAL_ID_PATTERNS = {
    "GB": re.compile(r"^[A-Z]{2}\d{6}[A-D]$"),
    "IE": re.compile(r"^\d{7}[A-W][A-IW]?$"),
    "NL": re.compile(r"^\d{9}$"),
    "ES": re.compile(r"^\d{8}[A-Z]$"),
}
REVIEW_OUTCOMES = {"approve": STATUS_VERIFIED, "reject": STATUS_REJECTED}


class IdentityValidationError(ValueError):
    def __init__(self, errors: list[str]):
        super().__init__("; ".join(errors))
        self.errors = errors


class IdentityStateError(RuntimeError):
    pass


def get(*, db_session: Session, identity_id: int) -> BorrowerIdentity | None:
    return db_session.get(BorrowerIdentity, identity_id)


def get_by_borrower(*, db_session: Session, borrower_id: int) -> BorrowerIdentity | None:
    return db_session.scalars(select(BorrowerIdentity).where(BorrowerIdentity.borrower_id == borrower_id)).first()


def list_pending(*, db_session: Session, limit: int = 50) -> list[BorrowerIdentity]:
    query = (
        select(BorrowerIdentity)
        .where(BorrowerIdentity.status == STATUS_PENDING)
        .order_by(BorrowerIdentity.created_at)
        .limit(limit)
    )
    return list(db_session.scalars(query))


def submit(*, db_session: Session, submission: IdentitySubmission) -> BorrowerIdentity:
    """Validate and store a borrower's identity details for manual review."""
    errors: list[str] = []
    full_name = (submission.full_name or "").strip()
    if len(full_name) < 2 or len(full_name) > 255:
        errors.append("full_name must be between 2 and 255 characters")
    email = (submission.email or "").strip().lower()
    if not EMAIL_PATTERN.match(email):
        errors.append("email is not a valid address")
    phone = re.sub(r"[\s()-]", "", submission.phone or "")
    if not PHONE_PATTERN.match(phone):
        errors.append("phone must be in international format")
    country = (submission.country or "").strip().upper()
    if country not in SUPPORTED_COUNTRIES:
        errors.append("country is not supported for onboarding")
    national_id = re.sub(r"[\s-]", "", submission.national_id or "").upper()
    if not NATIONAL_ID_PATTERNS.get(country, ANY_ID_PATTERN).match(national_id):
        errors.append("national_id does not match the format for the country")
    today = datetime.date.today()
    age = today.year - submission.date_of_birth.year - (
        (today.month, today.day) < (submission.date_of_birth.month, submission.date_of_birth.day)
    )
    if age < MINIMUM_AGE:
        errors.append("borrower must be at least 18 years old")
    if errors:
        raise IdentityValidationError(errors)

    existing = get_by_borrower(db_session=db_session, borrower_id=submission.borrower_id)
    if existing is not None and existing.status == STATUS_VERIFIED:
        raise IdentityStateError("borrower is already verified")

    identity = existing or BorrowerIdentity(borrower_id=submission.borrower_id)
    identity.full_name = full_name
    identity.email = email
    identity.phone = phone
    identity.date_of_birth = submission.date_of_birth
    identity.country = country
    identity.national_id = national_id
    identity.status = STATUS_PENDING
    identity.reviewed_at = None
    db_session.add(identity)
    db_session.commit()

    logger.info(
        "identity submitted borrower_id=%s email=%s phone=%s", submission.borrower_id, email, phone
    )
    return identity


def review(*, db_session: Session, identity: BorrowerIdentity, reviewer_id: int, outcome: str, note: str = "") -> IdentityReview:
    """Record an agent's decision. Only pending identities can be reviewed."""
    new_status = REVIEW_OUTCOMES.get(outcome)
    if new_status is None:
        raise IdentityValidationError([f"outcome must be one of {sorted(REVIEW_OUTCOMES)}"])
    if identity.status != STATUS_PENDING:
        raise IdentityStateError(f"identity {identity.id} is {identity.status}, not pending")

    entry = IdentityReview(reviewer_id=reviewer_id, outcome=new_status, note=note[:500])
    identity.reviews.append(entry)
    identity.status = new_status
    identity.reviewed_at = datetime.datetime.now(datetime.timezone.utc)
    db_session.commit()

    logger.info("identity %s reviewed by %s -> %s", identity.id, reviewer_id, new_status)
    return entry
