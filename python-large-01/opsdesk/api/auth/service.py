from sqlalchemy.orm import Session

from opsdesk.api.auth.models import StaffUser


def get(*, db_session: Session, user_id: int) -> StaffUser | None:
    return db_session.get(StaffUser, user_id)
