"""Identity verification endpoints for the onboarding flow and the review queue."""

import datetime
import re

from fastapi import APIRouter, HTTPException, status

from opsdesk.db import CurrentUser, DbSession, PrimaryKey
from opsdesk.part6.analytics.client import funnel_analytics
from opsdesk.part6.kyc import intake
from opsdesk.part6.kyc.intake import (
    ANY_ID_PATTERN,
    EMAIL_PATTERN,
    MINIMUM_AGE,
    NATIONAL_ID_PATTERNS,
    PHONE_PATTERN,
    SUPPORTED_COUNTRIES,
    IdentityStateError,
    IdentityValidationError,
)
from opsdesk.part6.kyc.models import BorrowerIdentityRead, IdentitySubmission, ReviewDecision
from opsdesk.part6.kyc.notices import send_verification_notice

router = APIRouter()


def _not_found() -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_404_NOT_FOUND,
        detail=[{"msg": "An identity check with this id does not exist."}],
    )


@router.post("/precheck", response_model=None)
def precheck_identity(submission: IdentitySubmission):
    """Validate a submission without storing it, so the form can show errors inline."""
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
    return {"valid": not errors, "errors": errors}


@router.post("", response_model=BorrowerIdentityRead, status_code=status.HTTP_201_CREATED)
def submit_identity(db_session: DbSession, submission: IdentitySubmission):
    try:
        identity = intake.submit(db_session=db_session, submission=submission)
    except IdentityValidationError as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=[{"msg": message} for message in exc.errors],
        ) from exc
    except IdentityStateError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=[{"msg": str(exc)}]) from exc

    funnel_analytics.track_identity_submitted(identity)
    send_verification_notice(identity)
    return identity


@router.get("/pending", response_model=list[BorrowerIdentityRead])
def list_pending_identities(db_session: DbSession, current_user: CurrentUser):
    return intake.list_pending(db_session=db_session)


@router.get("/{identity_id}", response_model=BorrowerIdentityRead)
def get_identity(db_session: DbSession, current_user: CurrentUser, identity_id: PrimaryKey):
    identity = intake.get(db_session=db_session, identity_id=identity_id)
    if identity is None:
        raise _not_found()
    return identity


@router.post("/{identity_id}/review", response_model=BorrowerIdentityRead)
def review_identity(
    db_session: DbSession, current_user: CurrentUser, identity_id: PrimaryKey, decision: ReviewDecision
):
    identity = intake.get(db_session=db_session, identity_id=identity_id)
    if identity is None:
        raise _not_found()
    try:
        intake.review(
            db_session=db_session,
            identity=identity,
            reviewer_id=current_user.id,
            outcome=decision.outcome,
            note=decision.note,
        )
    except IdentityValidationError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=[{"msg": str(exc)}]) from exc
    except IdentityStateError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=[{"msg": str(exc)}]) from exc

    funnel_analytics.track_identity_reviewed(identity)
    return identity
