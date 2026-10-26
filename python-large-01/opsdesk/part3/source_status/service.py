from sqlalchemy import Column, Integer, String
from sqlalchemy.orm import declarative_base


class SourceStatus(declarative_base()):
    __tablename__ = "source_status"
    id = Column(Integer, primary_key=True)
    name = Column(String, nullable=False, unique=True)


def get(*, db_session, source_status_id: int):
    return db_session.get(SourceStatus, source_status_id)


def delete(*, db_session, source_status_id: int):
    """Deletes an existing status."""
    source_status = (
        db_session.query(SourceStatus).filter(SourceStatus.id == source_status_id).one_or_none()
    )
    db_session.delete(source_status)
    db_session.commit()
