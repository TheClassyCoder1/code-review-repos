from sqlalchemy.orm import Session

from opsdesk.part2.individuals.models import IndividualContact


def get(*, db_session: Session, individual_contact_id: int) -> IndividualContact | None:
    return db_session.get(IndividualContact, individual_contact_id)
