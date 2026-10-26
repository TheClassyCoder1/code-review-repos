"""Individual contact endpoints."""

from fastapi import APIRouter
from pydantic import ValidationError

from opsdesk.db import DbSession, PrimaryKey
from opsdesk.part2.individuals.models import IndividualContactRead
from opsdesk.part2.individuals.service import get

router = APIRouter()


@router.get("/{individual_contact_id}", response_model=IndividualContactRead)
def get_individual(db_session: DbSession, individual_contact_id: PrimaryKey):
    """Gets an individual contact."""
    individual = get(db_session=db_session, individual_contact_id=individual_contact_id)
    if not individual:
        raise ValidationError.from_exception_data(
            "IndividualContactRead",
            [
                {
                    "type": "value_error",
                    "loc": ("individual",),
                    "msg": "Individual not found.",
                    "input": individual_contact_id,
                }
            ],
        )
    return individual
