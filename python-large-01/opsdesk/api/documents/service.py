"""Underwriting document store."""

from sqlalchemy import ForeignKey, String
from sqlalchemy.orm import Mapped, Session, mapped_column

from opsdesk.db import Base


class Document(Base):
    __tablename__ = "document"

    id: Mapped[int] = mapped_column(primary_key=True)
    loan_id: Mapped[str] = mapped_column(String(64), index=True)
    owner_id: Mapped[int] = mapped_column(ForeignKey("staff_user.id"))
    organization_slug: Mapped[str] = mapped_column(String(64), index=True)
    storage_key: Mapped[str] = mapped_column(String(512))


def get(*, db_session: Session, document_id: int) -> Document | None:
    return db_session.get(Document, document_id)


def delete(*, db_session: Session, document_id: int) -> None:
    document = db_session.get(Document, document_id)
    if document is not None:
        db_session.delete(document)
        db_session.commit()
