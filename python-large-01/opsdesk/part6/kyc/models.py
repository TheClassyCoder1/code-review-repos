"""Persistence for borrower identity checks."""

import datetime

from pydantic import BaseModel, ConfigDict
from sqlalchemy import Date, DateTime, ForeignKey, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from opsdesk.db import Base

STATUS_PENDING = "PENDING"
STATUS_VERIFIED = "VERIFIED"
STATUS_REJECTED = "REJECTED"
STATUSES = frozenset({STATUS_PENDING, STATUS_VERIFIED, STATUS_REJECTED})


class BorrowerIdentity(Base):
    __tablename__ = "borrower_identity"

    id: Mapped[int] = mapped_column(primary_key=True)
    borrower_id: Mapped[int] = mapped_column(index=True, unique=True)
    full_name: Mapped[str] = mapped_column(String(255))
    email: Mapped[str] = mapped_column(String(255))
    phone: Mapped[str] = mapped_column(String(32))
    date_of_birth: Mapped[datetime.date] = mapped_column(Date)
    country: Mapped[str] = mapped_column(String(2))
    national_id: Mapped[str] = mapped_column(String(32), index=True)
    status: Mapped[str] = mapped_column(String(16), default=STATUS_PENDING)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    reviewed_at: Mapped[datetime.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    reviews: Mapped[list["IdentityReview"]] = relationship(back_populates="identity", cascade="all, delete-orphan")


class IdentityReview(Base):
    __tablename__ = "borrower_identity_review"

    id: Mapped[int] = mapped_column(primary_key=True)
    identity_id: Mapped[int] = mapped_column(ForeignKey("borrower_identity.id"))
    reviewer_id: Mapped[int]
    outcome: Mapped[str] = mapped_column(String(16))
    note: Mapped[str] = mapped_column(String(500), default="")
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    identity: Mapped[BorrowerIdentity] = relationship(back_populates="reviews")


class BorrowerIdentityRead(BaseModel):
    """Agent-facing view. Contact details and the national id are never returned."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    borrower_id: int
    country: str
    status: str
    created_at: datetime.datetime
    reviewed_at: datetime.datetime | None = None


class IdentitySubmission(BaseModel):
    borrower_id: int
    full_name: str
    email: str
    phone: str
    date_of_birth: datetime.date
    country: str
    national_id: str


class ReviewDecision(BaseModel):
    outcome: str
    note: str = ""
